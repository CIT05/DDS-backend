package com.example.chatty_be;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;

import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;

    // use the class-level pointMgr, not a local variable
    private PointAnnotationManager pointMgr;
    private PointAnnotation        marker;

    private double pickedLat, pickedLng;
    private Button doneBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        mapView = findViewById(R.id.mapView);
        doneBtn = findViewById(R.id.btnDone);
        doneBtn.setEnabled(false);

        mapboxMap = mapView.getMapboxMap();
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
            // 1) init the annotation plugin + manager
            AnnotationPlugin annotationPlugin =
                    (AnnotationPlugin) mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
            pointMgr = (PointAnnotationManager) annotationPlugin.createAnnotationManager(
                    AnnotationType.PointAnnotation,
                    new AnnotationConfig()  // default config
            );

            // 2) wire up single-tap
            GesturesPlugin gestures =
                    (GesturesPlugin) mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
            gestures.addOnMapClickListener(new OnMapClickListener() {
                @Override
                public boolean onMapClick(@NonNull Point point) {
                    // Log to Logcat
                    Log.d("MapPickerActivity",
                            "Map clicked at: " + point.latitude() + ", " + point.longitude());
                    // Toast for visual feedback
                    Toast.makeText(MapPickerActivity.this,
                                    String.format(Locale.US,
                                            "Tap at %.5f, %.5f",
                                            point.latitude(),
                                            point.longitude()),
                                    Toast.LENGTH_SHORT)
                            .show();
                    // drop or move the pin
                    dropOrMovePin(point);
                    return true; // consume event
                }
            });
        });

        // 3) doneBtn separate from map listener
        doneBtn.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("lat", pickedLat);
            data.putExtra("lng", pickedLng);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void dropOrMovePin(Point point) {
        // remove old marker
        if (marker != null) {
            pointMgr.delete(marker);
        }

        // prepare your red-marker bitmap
        Drawable drawable = ResourcesCompat.getDrawable(
                getResources(), R.drawable.red_marker, null);
        Bitmap icon = drawableToBitmap(drawable);

        // create options & add the annotation
        PointAnnotationOptions opts = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(icon)
                .withIconSize(1.2f);
        marker    = pointMgr.create(opts);

        // cache for Done button
        pickedLat = point.latitude();
        pickedLng = point.longitude();
        doneBtn.setEnabled(true);
    }

    private Bitmap drawableToBitmap(Drawable d) {
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable) d).getBitmap();
        }
        Bitmap bmp = Bitmap.createBitmap(
                d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        return bmp;
    }

    // MapView lifecycle
    @Override protected void onStart()    { super.onStart();   mapView.onStart();  }
    @Override protected void onStop()     { super.onStop();    mapView.onStop();   }
    @Override protected void onDestroy()  { super.onDestroy(); mapView.onDestroy();}
    @Override public   void onLowMemory() { super.onLowMemory(); mapView.onLowMemory();}
}
