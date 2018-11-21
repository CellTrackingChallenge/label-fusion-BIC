package de.mpicbg.ulman.Mastodon;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.ui.util.FileChooser;
import org.mastodon.revised.ui.util.ExtensionFileFilter;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;
import net.imglib2.type.numeric.integer.UnsignedShortType;

@Plugin( type = CTC_Plugins.class )
public class CTC_Plugins extends AbstractContextual implements MastodonPlugin
{
	//"IDs" of all plug-ins wrapped in this class
	private static final String CTC_IMPORT = "CTC-import-all";
	private static final String CTC_EXPORT = "CTC-export-all";
	//------------------------------------------------------------------------

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		//this places the plug-in's menu items into the menu,
		//the titles of the items are defined right below
		return Arrays.asList(
				menu( "Plugins",
						menu( "Cell Tracking Challenge",
								item( CTC_IMPORT ), item ( CTC_EXPORT) ) ) );
	}

	/** titles of this plug-in's menu items */
	private static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put( CTC_IMPORT, "Import from CTC format" );
		menuTexts.put( CTC_EXPORT, "Export to CTC format" );
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}
	//------------------------------------------------------------------------

	private final AbstractNamedAction actionImport;
	private final AbstractNamedAction actionExport;

	/** default c'tor: creates Actions available from this plug-in */
	public CTC_Plugins()
	{
		actionImport = new RunnableAction( CTC_IMPORT, this::importer );
		actionExport = new RunnableAction( CTC_EXPORT, this::exporter );
		updateEnabledActions();
	}

	/** register the actions to the application (with no shortcut keys) */
	@Override
	public void installGlobalActions( final Actions actions )
	{
		final String[] noShortCut = new String[] {};
		actions.namedAction( actionImport, noShortCut );
		actions.namedAction( actionExport, noShortCut );
	}

	/** reference to the currently available project in Mastodon */
	private MastodonPluginAppModel pluginAppModel;

	/** learn about the current project's params */
	@Override
	public void setAppModel( final MastodonPluginAppModel model )
	{
		//the application reports back to us if some project is available
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		actionImport.setEnabled( appModel != null );
		actionExport.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------

	/** opens the import dialog to find the tracks.txt file,
	    and runs the import on the currently viewed images
	    provided params were harvested successfully */
	private void importer()
	{
		//open a folder choosing dialog
		File selectedFile = FileChooser.chooseFile(null, null,
				new ExtensionFileFilter("txt"),
				"Choose tracks.txt lineage file that corresponds to the current data:",
				FileChooser.DialogType.LOAD,
				FileChooser.SelectionMode.FILES_ONLY);

		//cancel button ?
		if (selectedFile == null) return;

		//check we can open the file; and complain if not
		if (selectedFile.canRead() == false)
			throw new IllegalArgumentException("Cannot read the selected lineage file: "+selectedFile.getAbsolutePath());

		ImporterPlugin ip = new ImporterPlugin();
		ip.setContext(this.getContext());

		ip.inputPath = selectedFile.getAbsolutePath();
		ip.imgSource = pluginAppModel.getAppModel().getSharedBdvData().getSources().get(0).getSpimSource();

		ip.model     = pluginAppModel.getAppModel().getModel();
		ip.timeFrom  = pluginAppModel.getAppModel().getMinTimepoint();
		ip.timeTill  = pluginAppModel.getAppModel().getMaxTimepoint();

		ip.run();
	}

	/** opens the export dialog, and runs the export
	    provided params were harvested successfully */
	private void exporter()
	{
		//open a folder choosing dialog
		File selectedFolder = FileChooser.chooseFile(null, null, null,
				"Choose GT folder with TRA folder inside:",
				FileChooser.DialogType.LOAD,
				FileChooser.SelectionMode.DIRECTORIES_ONLY);

		//cancel button ?
		if (selectedFolder == null) return;

		//check there is a TRA sub-folder; and if not, create it
		final File traFolder = new File(selectedFolder.getPath()+File.separator+"TRA");
		if (traFolder.exists())
		{
			//"move" into the existing TRA folder
			selectedFolder = traFolder;
		}
		else
		{
			if (traFolder.mkdirs())
				selectedFolder = traFolder;
			else
				throw new IllegalArgumentException("Cannot create missing subfolder TRA in the folder: "+selectedFolder.getAbsolutePath());
		}

		ExporterPlugin<UnsignedShortType> ep = new ExporterPlugin<>(new UnsignedShortType());
		ep.setContext(this.getContext());

		ep.outputPath = selectedFolder.getAbsolutePath();
		ep.imgSource  = pluginAppModel.getAppModel().getSharedBdvData().getSources().get(0).getSpimSource();
		ep.doOneZslicePerMarker = true;

		ep.model      = pluginAppModel.getAppModel().getModel();
		ep.timeFrom   = pluginAppModel.getAppModel().getMinTimepoint();
		ep.timeTill   = pluginAppModel.getAppModel().getMaxTimepoint();

		ep.run();
	}
}
