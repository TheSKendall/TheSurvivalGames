package com.communitysurvivalgames.thesurvivalgames.io;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ProgressListener implements ActionListener {

	Player p;
	String bytes;
	
	public ProgressListener(Player p, String b) {
		this.p = p;
		this.bytes = b;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		p.sendMessage(ChatColor.YELLOW + "Downloading map progress: Downloaded " + ChatColor.RED + ((DownloadCountingOutputStream) e.getSource()).getByteCount() + "/" + bytes + ChatColor.YELLOW + " bytes");
	}
}