package moe.knox.factorio.intellij;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.text.SemVer;
import moe.knox.factorio.core.parser.ApiParser;
import moe.knox.factorio.core.parser.LuaLibParser;
import moe.knox.factorio.core.parser.PrototypeParser;
import moe.knox.factorio.intellij.FactorioAutocompletionState.FactorioVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class FactorioAutocompletionConfig implements SearchableConfigurable {
    Project project;
    private FactorioAutocompletionState config;
    private JPanel rootPanel;
    private JCheckBox enableFactorioIntegrationCheckBox;
    private JComboBox<FactorioVersion> selectApiVersion;
    private JLabel loadError;
    private JButton reloadButton;

    public FactorioAutocompletionConfig(@NotNull Project project) {
        this.project = project;
        config = FactorioAutocompletionState.getInstance(project);

        enableFactorioIntegrationCheckBox.setSelected(config.integrationActive);


        try {
            getVersions().forEach(v -> selectApiVersion.addItem(v));
            selectApiVersion.setSelectedItem(config.selectedFactorioVersion);

            // hide error message
            selectApiVersion.setEnabled(true);
            loadError.setVisible(false);
            enableFactorioIntegrationCheckBox.setEnabled(true);
            reloadButton.setEnabled(config.integrationActive);
        } catch (Exception e) {
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

    private Set<FactorioVersion> getVersions() throws IOException {
        Set<FactorioVersion> result = new HashSet<>();
        result.add(FactorioVersion.createLatest());

        Document mainPageDoc = Jsoup.connect(ApiParser.factorioApiBaseLink).get();
        Elements allLinks = mainPageDoc.select("a");
        for (Element link : allLinks) {
            var semVer = SemVer.parseFromText(link.text());
            if (semVer == null) {
                continue;
            }

            var factorioVersion = FactorioVersion.createVersion(semVer.getRawVersion());

            selectApiVersion.addItem(factorioVersion);
        }

        return result;
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
        return selectApiVersion.isEnabled() && (config.integrationActive != enableFactorioIntegrationCheckBox.isSelected()
                || !config.selectedFactorioVersion.equals(selectApiVersion.getSelectedItem()));
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

        if (!config.selectedFactorioVersion.equals(selectApiVersion.getSelectedItem())) {
            // New Factorio Version selected
            // remove old apis
            ApiParser.removeCurrentAPI(project);
            LuaLibParser.removeCurrentLualib(project);

            // reload the lualib
            LuaLibParser.checkForUpdate(project);

            // save new settings
            if (selectApiVersion.getSelectedItem() != null) {
                config.selectedFactorioVersion = (FactorioVersion) selectApiVersion.getSelectedItem();
            }
        }

        reloadButton.setEnabled(enableIntegration);

        WriteAction.run(() -> FactorioLibraryProvider.reload());
    }
}
