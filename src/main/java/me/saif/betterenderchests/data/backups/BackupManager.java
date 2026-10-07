package me.saif.betterenderchests.data.backups;

import me.saif.betterenderchests.OberonEnder;
import me.saif.betterenderchests.utils.Manager;

import java.io.File;

public class BackupManager extends Manager<OberonEnder> {

    private File backupsFolder;

    public BackupManager(OberonEnder plugin) {
        super(plugin);
        this.backupsFolder = new File(plugin.getDataFolder(), "backups");
    }




}
