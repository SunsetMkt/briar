package net.sf.briar.ui.setup;

import java.io.File;

import net.sf.briar.api.i18n.FontManager;
import net.sf.briar.api.setup.SetupParameters;

class SetupParametersImpl implements SetupParameters {

	private final LocationPanel locationPanel;
	private final FontManager fontManager;

	SetupParametersImpl(LocationPanel locationPanel, FontManager fontManager) {
		this.locationPanel = locationPanel;
		this.fontManager = fontManager;
	}

	public File getChosenLocation() {
		return locationPanel.getChosenDirectory();
	}

	public String[] getBundledFontFilenames() {
		return fontManager.getBundledFontFilenames();
	}
}
