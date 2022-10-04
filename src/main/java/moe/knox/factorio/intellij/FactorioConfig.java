package moe.knox.factorio.intellij;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.core.version.FactorioVersionResolver;
import moe.knox.factorio.intellij.service.ApiService;
import moe.knox.factorio.intellij.service.LuaLibService;
import moe.knox.factorio.intellij.service.PrototypeService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.io.IOException;
import java.util.Objects;

public class FactorioConfig implements SearchableConfigurable {
    Project project;
    private FactorioState config;
    private JPanel rootPanel;
    private JCheckBox enableFactorioIntegrationCheckBox;
    private JComboBox<DropdownVersion> selectApiVersion;
    private JLabel loadError;
    private JButton reloadButton;
    private final FactorioVersionResolver factorioVersionResolver;
    @NotNull
    private final FactorioVersion latestExistingVersion;

    public FactorioConfig(@NotNull Project project) throws IOException {
        this.project = project;
        config = FactorioState.getInstance(project);
        factorioVersionResolver = new FactorioVersionResolver();
        latestExistingVersion = factorioVersionResolver.supportedVersions().latestVersion();

        enableFactorioIntegrationCheckBox.setSelected(config.integrationActive);


        try {
            var latestDropdownVersion = DropdownVersion.createLatest();

            selectApiVersion.addItem(latestDropdownVersion);
            factorioVersionResolver
                    .supportedVersions()
                    .stream()
                    .sorted(Comparator.reverseOrder())
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
            removeParsedLibraries();
            updateLibraries();
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
    public void apply() {
        if (isIntegrationTurnedOff()) {
            removeParsedLibraries();
        }

        if (isVersionChanged()) {
            ApiService.getInstance(project).removeCurrentAPI();
            LuaLibService.getInstance(project).removeLuaLibFiles();
            LuaLibService.getInstance(project).checkForUpdate();
        }

        reloadButton.setEnabled(enableFactorioIntegrationCheckBox.isSelected());

        config.integrationActive = enableFactorioIntegrationCheckBox.isSelected();
        config.useLatestVersion = isUseLatestVersion();
        config.selectedFactorioVersion = getSelectedVersion();

        WriteAction.run(FactorioLibraryProvider::reload);
    }

    private void removeParsedLibraries()
    {
        ApiService.getInstance(project).removeCurrentAPI();
        PrototypeService.getInstance(project).removeCurrentPrototypes();
        LuaLibService.getInstance(project).removeLuaLibFiles();
    }

    private void updateLibraries()
    {
        ApiService.getInstance(project).checkForUpdate();
        PrototypeService.getInstance(project).checkForUpdate();
        LuaLibService.getInstance(project).checkForUpdate();
    }

    private boolean isUseLatestVersion() {
        return Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem()).isLatest();
    }

    private FactorioVersion getSelectedVersion() {
        var dropdownVersion = Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem());

        if (dropdownVersion.isLatest()) {
            return latestExistingVersion;
        }

        return FactorioVersion.createVersion(dropdownVersion.version);
    }

    private boolean isVersionChanged()
    {
        return !config.selectedFactorioVersion.equals(getSelectedVersion());
    }

    private boolean isIntegrationTurnedOff()
    {
        return config.integrationActive && !enableFactorioIntegrationCheckBox.isSelected();
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

        public static DropdownVersion fromApiVersion(FactorioVersion v)
        {
            return new DropdownVersion(v.version(), v.version());
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
