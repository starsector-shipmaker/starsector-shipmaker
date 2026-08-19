package shipeditor.components.viewer.layers.ship.data;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.RepresentationEnums.HullSize;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.HullStyle;
import shipeditor.utility.graphics.ColorUtilities;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ShipHull {

    private String hullID;

    private String hullName;

    private String styleID;

    private HullStyle hullStyle;

    private HullSize hullSize;

    private List<HullmodCSVEntry> builtInMods;

    private List<WingCSVEntry> builtInWings;

    private Color coversColor;

    private int viewOffset;

    private String hullFileName;

    public void setHullStyle(HullStyle style) {
        this.hullStyle = style;
        if (style != null) {
            this.styleID = style.getHullStyleID();
        }
    }

    public void initialize(HullSpecFile specFile) {
        if (specFile == null) {
            log.error("Attempted to initialize ShipHull with null HullSpecFile!");
            return;
        }
        this.hullName = specFile.getHullName();
        this.hullID = specFile.getHullId();
        if (specFile.getHullSize() == null) {
            log.warn("Missing hull size in spec file for {}, defaulting to DEFAULT", specFile.getHullId());
            this.hullSize = HullSize.DEFAULT;
        } else {
            try {
                this.hullSize = HullSize.valueOf(specFile.getHullSize());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid hull size '{}' in spec file for {}, defaulting to DEFAULT", specFile.getHullSize(), specFile.getHullId());
                this.hullSize = HullSize.DEFAULT;
            }
        }
        this.loadHullStyle(specFile);

        var deserializedCoversColor = specFile.getCoversColor();
        if (deserializedCoversColor != null && !deserializedCoversColor.isEmpty()) {
            this.coversColor = ColorUtilities.convertStringToColor(deserializedCoversColor);
        }

        this.viewOffset = specFile.getViewOffset();

        var dataRepository = SettingsManager.getGameData();
        if (dataRepository.isHullmodDataLoaded()) {
            this.loadBuiltInMods(specFile);
        }
        if (dataRepository.isWingDataLoaded()) {
            this.loadBuiltInWings(specFile);
        }

        Path specFilePath = specFile.getFilePath();
        if (specFilePath != null) {
            this.hullFileName = String.valueOf(specFilePath.getFileName());
        } else {
            this.hullFileName = "Not saved";
        }

    }

    private void loadHullStyle(HullSpecFile specFile) {
        this.styleID = specFile.getStyle();
        this.hullStyle = GameDataRepository.fetchStyleByID(styleID);
    }

    public void loadBuiltInMods(HullSpecFile specFile) {
        if (builtInMods != null) return;
        String[] specFileBuiltInMods = specFile.getBuiltInMods();
        if (specFileBuiltInMods == null) {
            this.builtInMods = new ArrayList<>();
            return;
        }
        var gameData = SettingsManager.getGameData();
        var allHullmodEntries = gameData.getAllHullmodEntries();
        List<HullmodCSVEntry> builtInList = new ArrayList<>(specFileBuiltInMods.length);
        Stream<String> stream = Arrays.stream(specFileBuiltInMods);
        stream.forEach(hullmodID -> {
            HullmodCSVEntry hullmodEntry = allHullmodEntries.get(hullmodID);
            if (hullmodEntry != null) {
                builtInList.add(hullmodEntry);
            } else {
                log.error("Hullmod CSV entry not found for hullmod ID: {}", hullmodID);
            }
        });
        this.builtInMods = builtInList;
    }

    public void loadBuiltInWings(HullSpecFile specFile) {
        if (builtInWings != null) return;
        String[] specFileBuiltInWings = specFile.getBuiltInWings();
        if (specFileBuiltInWings == null) {
            this.builtInWings = new ArrayList<>();
            return;
        }
        var gameData = SettingsManager.getGameData();
        var allWingEntries = gameData.getAllWingEntries();
        List<WingCSVEntry> builtInList = new ArrayList<>(specFileBuiltInWings.length);
        Stream<String> stream = Arrays.stream(specFileBuiltInWings);
        stream.forEach(wingID -> {
            WingCSVEntry wingCSVEntry = allWingEntries.get(wingID);
            if (wingCSVEntry != null) {
                builtInList.add(wingCSVEntry);
            } else {
                log.error("Wing CSV entry not found for wing ID: {}", wingID);
            }
        });
        this.builtInWings = builtInList;
    }

}
