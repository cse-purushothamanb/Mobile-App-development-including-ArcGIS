package com.example.finalapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

// Base class for activities that use the support library action bar features
import androidx.appcompat.app.AppCompatActivity;
// Layout class that supports constraints for child views, enabling flexible UI designs
import androidx.constraintlayout.widget.ConstraintLayout;
// Helper for accessing features in ActivityCompat in a backward compatible fashion
import androidx.core.app.ActivityCompat;
// Helper for accessing features in ContextCompat in a backward compatible fashion
import androidx.core.content.ContextCompat;
// Base class for Dialogs that show a progress indicator and an optional message or view
import android.app.ProgressDialog;
// Interface used to allow the creator of a dialog to run some code when an alert dialog is dismissed
import android.content.DialogInterface;
// Component for an operation to be performed in a different process or application
import android.content.Intent;
// Package manager class for retrieving various kinds of information related to the application packages that are currently installed on the device
import android.content.pm.PackageManager;
// Class for handling operations on Uri objects, which represent abstract data
import android.net.Uri;
// Base class for activities to hold common creation and destruction behaviors
import android.os.Bundle;
// API for sending log output
import android.util.Log;
// Basic building block for user interface components
import android.view.View;
// Widget that enables the user to take action, with or without text inside
import android.widget.Button;
// A button with an icon (image) that can be pressed or clicked by the user
import android.widget.ImageButton;
// Class for showing a brief message on the screen
import android.widget.Toast;
// Class for manifest permission constants
import android.Manifest;
// Provides a set of static methods and properties to set the license information for ArcGIS Runtime and to get information about the license
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
// Represents a job that can be monitored, cancelled, and queried for status
import com.esri.arcgisruntime.concurrent.Job;
// An interface for classes that listen for changes to a ListenableFuture
import com.esri.arcgisruntime.concurrent.ListenableFuture;
// Represents a local tile cache on disk created by exporting tiles from a service
import com.esri.arcgisruntime.data.TileCache;
// Defines a rectangular area in map coordinates
import com.esri.arcgisruntime.geometry.Envelope;
// Represents a location on the Earth's surface
import com.esri.arcgisruntime.geometry.Point;
// A layer that displays tiles from an ArcGIS or OGC tile service
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
// Represents a map in ArcGIS Runtime
import com.esri.arcgisruntime.mapping.ArcGISMap;
// Represents the base layer of a map
import com.esri.arcgisruntime.mapping.Basemap;
// Enumeration of the available basemap styles
import com.esri.arcgisruntime.mapping.BasemapStyle;
// Represents a viewpoint, or a specific way of looking at a map
import com.esri.arcgisruntime.mapping.Viewpoint;
// Controls the display of the device's current location on the map
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
// A view that displays a map or scene by using a MapView or SceneView
import com.esri.arcgisruntime.mapping.view.MapView;
// Represents a job that exports tiles to create a tile cache
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheJob;
// Parameters that define how a tile cache is exported
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheParameters;
// A task used to export tiles to a tile cache
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheTask;
// Java file class for file manipulation
import java.io.File;
// Java class for writing to files
import java.io.FileOutputStream;
// Abstract class representing an input stream of bytes

import java.io.InputStream;
// Abstract class representing an output stream of bytes
import java.io.OutputStream;
// Exception thrown when an attempt to retrieve the result of a task ends, because the task completed exceptionally
import java.util.concurrent.ExecutionException;
// For Input output Exceptions
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private MapView mMapView;
    private View mPreviewMask;
    private Button mExportTilesButton;
    private ExportTileCacheJob mExportTileCacheJob;
    private ExportTileCacheTask mExportTileCacheTask;
    private ConstraintLayout mTileCachePreviewLayout;
    private ImageButton Current_location;
    private LocationDisplay locationDisplay;
    private MapView mTileCachePreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView=findViewById(R.id.mapView);
        mPreviewMask=findViewById(R.id.previewMask);
        mExportTilesButton=findViewById(R.id.exportTilesButton);
        mTileCachePreview = findViewById(R.id.previewMapView);
        mTileCachePreviewLayout = findViewById(R.id.mapPreviewLayout);
        Current_location=findViewById(R.id.current_location);
        //Import button
        Button importTilesButton = findViewById(R.id.importTilesButton);
        importTilesButton.setOnClickListener(v -> openFilePicker());
        ArcGISRuntimeEnvironment.setApiKey("AAPK8295f8c1f7df46c8847c95fa61d31070m-KBwornZP-D3bK9thEP2c2ve0_J-xfZrsv1I16idrL9j-DqTKYP2Ul-K_9AK0b8");
        ArcGISMap map = new ArcGISMap();
        map.setBasemap(new Basemap(BasemapStyle.ARCGIS_IMAGERY));
        //BasemapStyle.ARCGIS_IMAGERY
        map.setMinScale(10000000);
        Current_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDisplay != null && locationDisplay.getLocation() != null) {
                    Point currentLocation = locationDisplay.getLocation().getPosition();
                    mMapView.setViewpointCenterAsync(currentLocation);
                }
            }
        });
        mMapView.setMap(map);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        } else {
            // Start location display
            setupLocationDisplay();
        }
        //mMapView.setViewpoint(new Viewpoint(17 , -17, 10000000.0));
        mExportTilesButton = findViewById(R.id.exportTilesButton);
        mExportTilesButton.setOnClickListener(v -> initiateDownload());
        Button previewCloseButton = findViewById(R.id.closeButton);
        previewCloseButton.setOnClickListener(v -> clearPreview());
        //clearPreview();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TILE_CACHE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                loadMapFromTileCacheUri(uri); // You'll need to implement this method
            }
        }
    }

    private void loadMapFromTileCacheUri(Uri tileCacheUri) {
        try {
            // Create a temporary file in your app's private storage
            File tempFile = File.createTempFile("tileCache", ".tpkx", getExternalFilesDir(null));
            try (InputStream inputStream = getContentResolver().openInputStream(tileCacheUri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {
                // Copy the content from the Uri to the temporary file
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error copying tile cache to temporary file: " + e.getMessage(), e);
                return;
            }

            // Load the tile cache from the temporary file
            TileCache tileCache = new TileCache(tempFile.getAbsolutePath());
            ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tileCache);
            ArcGISMap map = new ArcGISMap(new Basemap(tiledLayer));
            mMapView.setMap(map);
        } catch (Exception e) {
            Log.e(TAG, "Error loading tile cache: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading tile cache", Toast.LENGTH_LONG).show();
        }
    }



    private void setupLocationDisplay() {
        locationDisplay = mMapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        locationDisplay.startAsync();
    }
    private static final int PICK_TILE_CACHE_FILE = 1; // Request code for picking a file

    private void openFilePicker() {
        // Example of guiding users
        Toast.makeText(this, "Please navigate to the folder where your .tpkx files are stored.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Use a general type since specific MIME types for .tpkx may not be recognized
        startActivityForResult(intent, PICK_TILE_CACHE_FILE);
    }


    private void clearPreview() {
        // make map preview invisible
        mTileCachePreview.getChildAt(0).setVisibility(View.INVISIBLE);
        mMapView.bringToFront();
        // show red preview mask
        mPreviewMask.bringToFront();
        mExportTilesButton.setVisibility(View.VISIBLE);
    }
    private Envelope viewToExtent() {
        // upper left corner of the downloaded tile cache area
        android.graphics.Point minScreenPoint = new android.graphics.Point(mMapView.getLeft() - mMapView.getWidth(),
                mMapView.getTop() - mMapView.getHeight());
        // lower right corner of the downloaded tile cache area
        android.graphics.Point maxScreenPoint = new android.graphics.Point(minScreenPoint.x + mMapView.getWidth() * 3,
                minScreenPoint.y + mMapView.getHeight() * 3);
        // convert screen points to map points
        Point minPoint = mMapView.screenToLocation(minScreenPoint);
        Point maxPoint = mMapView.screenToLocation(maxScreenPoint);
        // use the points to define and return an envelope
        return new Envelope(minPoint, maxPoint);
    }
    private void createProgressDialog(ExportTileCacheJob exportTileCacheJob) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Export Tile Cache Job");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                (dialogInterface, i) -> exportTileCacheJob.cancelAsync());
        progressDialog.show();

        exportTileCacheJob.addProgressChangedListener(() -> progressDialog.setProgress(exportTileCacheJob.getProgress()));
        exportTileCacheJob.addJobDoneListener(progressDialog::dismiss);
    }
    private void showMapPreview(TileCache result) {
        ArcGISTiledLayer newTiledLayer = new ArcGISTiledLayer(result);
        ArcGISMap map = new ArcGISMap(new Basemap(newTiledLayer));
        mTileCachePreview.setMap(map);
        mTileCachePreview.setViewpoint(mMapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE));
        mTileCachePreview.setVisibility(View.VISIBLE);
        mTileCachePreviewLayout.bringToFront();
        mTileCachePreview.getChildAt(0).setVisibility(View.VISIBLE);
        mExportTilesButton.setVisibility(View.GONE);
    }
    private void initiateDownload() {
        ArcGISTiledLayer tiledLayer = (ArcGISTiledLayer) mMapView.getMap().getBasemap().getBaseLayers().get(0);
        mExportTileCacheTask = new ExportTileCacheTask(tiledLayer.getUri());
        final ListenableFuture<ExportTileCacheParameters> parametersFuture = mExportTileCacheTask
                .createDefaultExportTileCacheParametersAsync(viewToExtent(), mMapView.getMapScale(), mMapView.getMapScale() * 0.1);

        parametersFuture.addDoneListener(() -> {
            File exportFile = null; // Declare outside the try block to widen its scope
            try {
                ExportTileCacheParameters parameters = parametersFuture.get();

                File myTileCachesDir = new File(getExternalFilesDir(null), "MyTileCaches");
                if (!myTileCachesDir.exists()) {
                    boolean wasDirMade = myTileCachesDir.mkdirs();
                    Log.d(TAG, "Directory creation for tile cache: " + wasDirMade);
                }

                String uniqueFileName = "tileCache_" + System.currentTimeMillis() + ".tpkx";
                exportFile = new File(myTileCachesDir, uniqueFileName); // Initialize here

                mExportTileCacheJob = mExportTileCacheTask.exportTileCache(parameters, exportFile.getAbsolutePath());
                Log.d(TAG, "Exporting tile cache to: " + exportFile.getAbsolutePath());
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Error generating parameters: " + e.getMessage(), e);
            }
            if (exportFile != null) { // Ensure exportFile is not null before using
                final File finalExportFile = exportFile; // Effectively final for use in lambda
                mExportTileCacheJob.start();
                createProgressDialog(mExportTileCacheJob);

                mExportTileCacheJob.addJobDoneListener(() -> {
                    if (mExportTileCacheJob.getStatus() == Job.Status.SUCCEEDED && mExportTileCacheJob.getResult() != null) {
                        TileCache exportedTileCacheResult = mExportTileCacheJob.getResult();
                        showMapPreview(exportedTileCacheResult);
                        Log.d(TAG, "Export Tile Cache Job succeeded. File saved to: " + finalExportFile.getAbsolutePath());
                    } else {
                        Log.e(TAG, "Tile cache job failed or result is null. Status: " + mExportTileCacheJob.getStatus());
                        Toast.makeText(MainActivity.this, "Failed to export tile cache. Check log for details.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.e(TAG, "Export file was not created. Check file path and permissions.");
            }
        });
    }
}