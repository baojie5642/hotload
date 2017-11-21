package com.baojie.hotload;

import java.io.IOException;

public class Start {

	public static void main(String[] args) throws IOException {

		final ServerManager serverManager = ServerManager.getInstance();
		serverManager.registerNotification(new ServerManager.Notification() {
			@Override
			public void showdown() {
				System.out.println("********* roam server stopped *********");
			}
		});
		serverManager.startServer();
	}
}
