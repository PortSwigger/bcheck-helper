package ui.view.pane.storefront;

import bcheck.BCheck;
import bcheck.BCheck.Tags;
import settings.tags.TagColors;
import ui.controller.StoreController;
import ui.icons.IconFactory;
import ui.model.StorefrontModel;
import ui.model.table.BCheckTableModel;
import ui.view.utils.TagRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.concurrent.Executor;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.GridBagConstraints.NONE;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static ui.model.StorefrontModel.SEARCH_FILTER_CHANGED;

class TablePanel extends JPanel {
    private final StoreController storeController;
    private final JTable bCheckTable = new JTable();
    private final BCheckTableModel tableModel = new BCheckTableModel();
    private final JButton refreshButton = new JButton("Refresh");
    private final Executor executor;
    private final JComponent searchBar;
    private final StorefrontModel model;
    private final ActionController actionController;

    TablePanel(StoreController storeController,
               StorefrontModel storefrontModel,
               Executor executor,
               IconFactory iconFactory,
               ActionController actionController) {
        super(new BorderLayout());

        this.storeController = storeController;
        this.executor = executor;
        this.model = storefrontModel;
        this.actionController = actionController;
        this.searchBar = new SearchBar(iconFactory, storefrontModel);

        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(10);
        setLayout(borderLayout);

        setupTablePanel();

        model.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(SEARCH_FILTER_CHANGED)) {
                tableModel.setBChecks(model.getFilteredBChecks());
            }
        });

        loadBChecks();
    }

    private void setupTablePanel() {
        model.setSearchFilter("");
        tableModel.setBChecks(model.getFilteredBChecks());

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                invokeLater(() -> {
                    int rowAtPoint = bCheckTable.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), bCheckTable));
                    if (rowAtPoint > -1) {
                        bCheckTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                    }
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        JMenuItem copyBCheckMenuItem = new JMenuItem("Copy BCheck");
        copyBCheckMenuItem.addActionListener(l -> actionController.copySelectedBCheck());

        JMenuItem saveBCheckMenuItem = new JMenuItem("Save BCheck");
        saveBCheckMenuItem.addActionListener(l -> actionController.saveSelectedBCheck());

        popupMenu.add(copyBCheckMenuItem);
        popupMenu.add(saveBCheckMenuItem);
        bCheckTable.setComponentPopupMenu(popupMenu);

        bCheckTable.setModel(tableModel);
        bCheckTable.setAutoCreateRowSorter(true);
        bCheckTable.setSelectionMode(SINGLE_SELECTION);
        bCheckTable.getTableHeader().setReorderingAllowed(false);
        bCheckTable.getSelectionModel().addListSelectionListener(e -> handleTableRowChange(e, tableModel));
        bCheckTable.setDefaultRenderer(Tags.class, new TagRenderer(new TagColors()));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());

        refreshButton.addActionListener(e -> loadBChecks());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 0, 0, 12);

        topPanel.add(searchBar, constraints);

        constraints.gridx = 1;
        constraints.weightx = 0;
        constraints.fill = NONE;
        constraints.insets = new Insets(10, 0, 0, 0);

        topPanel.add(refreshButton, constraints);

        add(topPanel, NORTH);
        add(new JScrollPane(bCheckTable), CENTER);
        setBorder(createEmptyBorder(0, 0, 5, 5));
    }

    private void loadBChecks() {
        refreshButton.setEnabled(false);

        executor.execute(() -> {
            model.setStatus("");
            storeController.loadData();
            tableModel.setBChecks(model.getFilteredBChecks());

            if (tableModel.getRowCount() > 0) {
                bCheckTable.addRowSelectionInterval(0, 0);
            }

            model.setStatus(storeController.status());
            refreshButton.setEnabled(true);
        });
    }

    private void handleTableRowChange(ListSelectionEvent selectionEvent, BCheckTableModel tableModel) {
        if (selectionEvent.getValueIsAdjusting()) {
            return;
        }

        int selectedRow = bCheckTable.getSelectedRow();

        BCheck selectedBCheck = selectedRow >= 0
                ? tableModel.getBCheckAtRow(bCheckTable.convertRowIndexToModel(selectedRow))
                : null;

        model.setSelectedBCheck(selectedBCheck);
    }
}
