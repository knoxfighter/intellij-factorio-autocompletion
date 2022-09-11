package moe.knox.factorio.intellij;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.core.parser.ApiParser;
import moe.knox.factorio.core.parser.LuaLibParser;
import moe.knox.factorio.core.parser.PrototypeParser;
import moe.knox.factorio.core.version.ApiVersionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Objects;

public class FactorioAutocompletionConfig implements SearchableConfigurable {
    Project project;
    private FactorioAutocompletionState config;
    private JPanel rootPanel;
    private JCheckBox enableFactorioIntegrationCheckBox;
    private JComboBox<DropdownVersion> selectApiVersion;
    private JLabel loadError;
    private JButton reloadButton;
    private final ApiVersionResolver apiVersionResolver;

    public FactorioAutocompletionConfig(@NotNull Project project) {
        this.project = project;
        config = FactorioAutocompletionState.getInstance(project);
        apiVersionResolver = new ApiVersionResolver();

        enableFactorioIntegrationCheckBox.setSelected(config.integrationActive);


        try {
            var latestDropdownVersion = DropdownVersion.createLatest();

            selectApiVersion.addItem(latestDropdownVersion);
            apiVersionResolver
                    .supportedVersions()
                    .stream()
                    .sorted(Collections.reverseOrder())
                    .map(DropdownVersion::fromApiVersion)
                    .forEach(v -> selectApiVersion.addItem(v))
            ;

            if (config.useLatestVersion) {
                selectApiVersion.setSelectedItem(latestDropdownVersion);
            } else {
                selectApiVersion.setSelectedItem(DropdownVersion.fromApiVersion(config.selectedFactorioVersion));
            }

            // hide error message
            selectApiVersion.setEnabled(true);
            loadError.setVisible(false);
            enableFactorioIntegrationCheckBox.setEnabled(true);
            reloadButton.setEnabled(config.integrationActive);
        }
        // todo catch only connection problems and wrote correct message
        catch (Exception e) {
            // show error message
            selectApiVersion.setEnabled(false);
            loadError.setText("Error loading Factorio versions. You need to have active internet connection to change these settings");
            loadError.setVisible(true);
            enableFactorioIntegrationCheckBox.setEnabled(false);
            reloadButton.setEnabled(false);
        }

        reloadButton.addActionListener(actionEvent -> {
            ApiParser.removeCurrentAPI(project);
            PrototypeParser.removeCurrentPrototypes();
            LuaLibParser.removeCurrentLualib(project);
            LuaLibParser.checkForUpdate(project);
            FactorioLibraryProvider.reload();
        });
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.FactorioCompletionConfig";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Factorio Autocompletion";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return rootPanel;
    }

    @Override
    public boolean isModified() {
        if (!selectApiVersion.isEnabled()) {
            return false;
        }

        return config.integrationActive != enableFactorioIntegrationCheckBox.isSelected()
                || !config.selectedFactorioVersion.equals(getSelectedVersion());
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean enableIntegration = enableFactorioIntegrationCheckBox.isSelected();

        if (!enableIntegration && config.integrationActive) {
            // integration deactivated
            ApiParser.removeCurrentAPI(project);
            PrototypeParser.removeCurrentPrototypes();
            LuaLibParser.removeCurrentLualib(project);
        }

        config.integrationActive = enableIntegration;

        if (!config.selectedFactorioVersion.equals(getSelectedVersion())) {
            // New Factorio Version selected
            // remove old apis
            ApiParser.removeCurrentAPI(project);
            LuaLibParser.removeCurrentLualib(project);

            // reload the lualib
            LuaLibParser.checkForUpdate(project);

            // save new settings
            if (selectApiVersion.getSelectedItem() != null) {
                config.selectedFactorioVersion = getSelectedVersion();
            }
        }

        reloadButton.setEnabled(enableIntegration);

        config.useLatestVersion = getUseLatestVersion();

        WriteAction.run(() -> FactorioLibraryProvider.reload());
    }

    private boolean getUseLatestVersion() {
        return Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem()).isLatest();
    }

    private FactorioApiVersion getSelectedVersion() {
        var dropdownVersion = Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem());

        return FactorioApiVersion.createVersion(dropdownVersion.version);
    }

    private record DropdownVersion(String version, String name) {
        public boolean isLatest()
        {
            return version.equals("latest");
        }

        public static DropdownVersion createLatest()
        {
            return new DropdownVersion("latest", "Latest version");
        }

        public static DropdownVersion fromApiVersion(FactorioApiVersion v)
        {
            return new DropdownVersion(v.version(), v.version());
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
