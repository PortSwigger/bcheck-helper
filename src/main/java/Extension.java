import bcheck.BCheckFactory;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import client.GitHubClient;
import file.finder.BCheckFileFinder;
import file.system.FileSystem;
import file.temp.TempFileCreator;
import file.zip.ZipExtractor;
import logging.Logger;
import network.RequestSender;
import repository.GitHubRepository;
import repository.Repository;
import settings.controller.SettingsController;
import ui.clipboard.ClipboardManager;
import ui.controller.StoreController;
import ui.icons.IconFactory;
import ui.model.DefaultStorefrontModel;
import ui.model.LateInitializationStorefrontModel;
import ui.model.StorefrontModel;
import ui.view.BCheckStore;
import ui.view.pane.settings.Settings;
import ui.view.pane.storefront.Storefront;
import utils.CloseablePooledExecutor;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class Extension implements BurpExtension {
    private static final String TAB_TITLE = "BCheck Helper";

    @Override
    public void initialize(MontoyaApi api) {
        Persistence persistence = api.persistence();
        SettingsController settingsController = new SettingsController(persistence.preferences());

        Logger logger = new Logger(api.logging(), settingsController.debugSettings());
        RequestSender requestSender = new RequestSender(api.http(), logger);
        BCheckFactory bCheckFactory = new BCheckFactory(logger);
        GitHubClient gitHubClient = new GitHubClient(requestSender);
        TempFileCreator tempFileCreator = new TempFileCreator(logger);
        ZipExtractor zipExtractor = new ZipExtractor(logger);
        BCheckFileFinder bCheckFileFinder = new BCheckFileFinder();
        Repository repository = new GitHubRepository(
                bCheckFactory,
                gitHubClient,
                tempFileCreator,
                zipExtractor,
                bCheckFileFinder,
                settingsController.gitHubSettings()
        );
        CloseablePooledExecutor executor = new CloseablePooledExecutor();

        IconFactory iconFactory = new IconFactory(api.userInterface());
        ClipboardManager clipboardManager = new ClipboardManager();
        FileSystem fileSystem = new FileSystem(logger);

        AtomicReference<StorefrontModel> modelReference = new AtomicReference<>();
        StorefrontModel lateInitializationStorefrontModel = new LateInitializationStorefrontModel(modelReference::get);
        StoreController storeController = new StoreController(
                lateInitializationStorefrontModel,
                repository,
                clipboardManager,
                fileSystem
        );

        StorefrontModel storefrontModel = new DefaultStorefrontModel(storeController);
        modelReference.set(storefrontModel);

        Settings settings = new Settings(settingsController);

        Storefront storefront = new Storefront(
                storeController,
                storefrontModel,
                settingsController.defaultSaveLocationSettings(),
                executor,
                iconFactory,
                logger,
                () -> api.userInterface().currentDisplayFont()
        );

        BCheckStore bcheckStore = new BCheckStore(settings, storefront);

        api.userInterface().registerSuiteTab(TAB_TITLE, bcheckStore);
        api.extension().registerUnloadingHandler(executor::close);
    }
}
