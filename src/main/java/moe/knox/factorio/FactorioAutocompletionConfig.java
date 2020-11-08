package moe.knox.factorio;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.downloader.DownloaderContainer;
import moe.knox.factorio.library.FactorioLibraryProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            for (String availableVersion : config.getAvailableVersions()) {
                selectApiVersion.addItem(availableVersion);
            }
            // also add latest to the list
            selectApiVersion.addItem("latest");

            selectApiVersion.setSelectedItem(config.selectedVersion);

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
            DownloaderContainer downloader = DownloaderContainer.getInstance(project);
            downloader.remove();
            downloader.download();
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
                || !config.selectedVersion.equals(selectApiVersion.getSelectedItem()));
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean enableIntegration = enableFactorioIntegrationCheckBox.isSelected();

        DownloaderContainer downloaderContainer = DownloaderContainer.getInstance(project);
        if (!enableIntegration && config.integrationActive) {
            // integration deactivated
            downloaderContainer.remove();
        }

        config.integrationActive = enableIntegration;

        Object selectedItem = selectApiVersion.getSelectedItem();
        if (!config.selectedVersion.equals(selectedItem) && selectedItem != null) {
            // New Factorio Version selected
            // save new settings
            config.selectedVersion = (String) selectedItem;

            // remove old apis
            downloaderContainer.remove();

            // download new apis
            downloaderContainer.download();
        }

        reloadButton.setEnabled(enableIntegration);

        WriteAction.run(() -> FactorioLibraryProvider.reload());
    }
}
