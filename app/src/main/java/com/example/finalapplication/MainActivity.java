package com.example.finalapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.Manifest;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheJob;
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheParameters;
import com.esri.arcgisruntime.tasks.tilecache.ExportTileCacheTask;

import java.util.concurrent.ExecutionException;

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
    private void setupLocationDisplay() {
        locationDisplay = mMapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        locationDisplay.startAsync();
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
        // initialize the export task
        mExportTileCacheTask = new ExportTileCacheTask(tiledLayer.getUri());
        final ListenableFuture<ExportTileCacheParameters> parametersFuture = mExportTileCacheTask
                .createDefaultExportTileCacheParametersAsync(viewToExtent(), mMapView.getMapScale(), mMapView.getMapScale() * 0.1);
        parametersFuture.addDoneListener(() -> {
            try {
                // export tile cache to directory
                ExportTileCacheParameters parameters = parametersFuture.get();
                mExportTileCacheJob = mExportTileCacheTask
                        .exportTileCache(parameters, getExternalCacheDir() + "/file1.tpkx");
                System.out.println("Parameter: "+parameters);
                System.out.println("getExternalCacheDir() : "+getExternalCacheDir());
            } catch (InterruptedException e) {
                Log.e(TAG, "TileCacheParameters interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                Log.e(TAG, "Error generating parameters: " + e.getMessage());
            }
            mExportTileCacheJob.start();

            createProgressDialog(mExportTileCacheJob);

            mExportTileCacheJob.addJobDoneListener(() -> {
                if (mExportTileCacheJob.getResult() != null) {
                    TileCache exportedTileCacheResult = mExportTileCacheJob.getResult();
                    showMapPreview(exportedTileCacheResult);
                    System.out.println("mExportTileCacheJob is not null");
                } else {
                    Log.e(TAG, "Tile cache job result null. File size may be too big.");
                    Toast.makeText(this,
                            "Tile cache job result null. File size may be too big. Try zooming in before exporting tiles",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}