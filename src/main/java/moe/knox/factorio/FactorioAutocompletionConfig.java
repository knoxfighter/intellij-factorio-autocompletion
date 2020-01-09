package moe.knox.factorio;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.library.FactorioApiParser;
import moe.knox.factorio.library.FactorioLibraryProvider;
import moe.knox.factorio.library.FactorioLualibParser;
import moe.knox.factorio.library.FactorioPrototypeParser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;

public class FactorioAutocompletionConfig implements SearchableConfigurable {
    Project project;
    private FactorioAutocompletionState config;
    private JPanel rootPanel;
    private JCheckBox enableFactorioIntegrationCheckBox;
    private JComboBox selectApiVersion;
    private JLabel loadError;
    private JButton reloadButton;

    public FactorioAutocompletionConfig(@NotNull Project project) {
        this.project = project;
        config = FactorioAutocompletionState.getInstance(project);

        enableFactorioIntegrationCheckBox.setSelected(config.integrationActive);


        try {
            Document mainPageDoc = Jsoup.connect(FactorioApiParser.factorioApiBaseLink).get();
            Elements allLinks = mainPageDoc.select("a");
            for (Element link : allLinks) {
                FactorioAutocompletionState.FactorioVersion factorioVersion = new FactorioAutocompletionState.FactorioVersion(link.text(), link.attr("href"));
                selectApiVersion.addItem(factorioVersion);
            }
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
            FactorioApiParser.removeCurrentAPI(project);
            FactorioPrototypeParser.removeCurrentPrototypes();
            FactorioLualibParser.removeCurrentLualib(project);
            FactorioLualibParser.checkForUpdate(project);
            FactorioLibraryProvider.reload();
        });
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.FactorioCompletionConfig";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
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
            FactorioApiParser.removeCurrentAPI(project);
            FactorioPrototypeParser.removeCurrentPrototypes();
            FactorioLualibParser.removeCurrentLualib(project);
        }

        config.integrationActive = enableIntegration;

        if (!config.selectedFactorioVersion.equals(selectApiVersion.getSelectedItem())) {
            // New Factorio Version selected
            // remove old apis
            FactorioApiParser.removeCurrentAPI(project);
            FactorioLualibParser.removeCurrentLualib(project);

            // reload the lualib
            FactorioLualibParser.checkForUpdate(project);

            // save new settings
            if (selectApiVersion.getSelectedItem() != null) {
                config.selectedFactorioVersion = (FactorioAutocompletionState.FactorioVersion) selectApiVersion.getSelectedItem();
            }
        }

        reloadButton.setEnabled(enableIntegration);

        WriteAction.run(() -> FactorioLibraryProvider.reload());
    }
}
