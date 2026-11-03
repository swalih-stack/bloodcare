package com.bloodcare.ui;

import com.bloodcare.db.InventoryDao;

import java.awt.*;

/** Blood Inventory page: stock tiles on top, the stock ledger (every add/remove) below. */
public class InventoryPage extends BasePage {
    private final StockTiles tiles = new StockTiles();
    private final DataPage log;

    public InventoryPage(Runnable afterChange) {
        super(new BorderLayout(0, 16));
        log = new DataPage(InventoryDao::log, () -> { tiles.reload(); afterChange.run(); },
                "ID", "Blood bank", "Group", "Units (+/-)", "Note", "Date");
        log.addAction("Add Blood Units", () -> AdminActions.addUnits(this));
        log.addAction("Remove Blood Units", () -> AdminActions.removeUnits(this));
        add(Ui.card(tiles), BorderLayout.NORTH);
        add(log, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        tiles.reload();
        log.refresh();
    }
}
