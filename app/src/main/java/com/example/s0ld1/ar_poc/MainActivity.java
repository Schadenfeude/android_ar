package com.example.s0ld1.ar_poc;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.example.s0ld1.ar_poc.utils.ARUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Light;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private boolean isArInstalled = false;
    private boolean hasFinishedLoading = false;
    private Snackbar loadingSnackbar;

    private ArSceneView arSceneView;
    private ModelRenderable andyRenderable;
    private ModelRenderable distilRenderable;
    private ModelRenderable hayRenderable;
    private ViewRenderable menuRenderable;

    private GestureDetector gestureDetector;
    private AnchorNode menuAnchorNode;
    private Anchor menuAnchor;

    private Map<Renderable, AnchorNode> displayedObjects = new HashMap<>();
    private Node animatedNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arSceneView = findViewById(R.id.ar_scene_view);

        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA}, 1);

        ARUtils.checkForARSupport(this, TAG);
        ARUtils.requestARInstall(this, isArInstalled);

        loadModels();

        arSceneView.getScene().addOnUpdateListener(frameTime -> {
            if (loadingSnackbar == null) {
                return;
            }

            final Frame frame = arSceneView.getArFrame();
            if (frame == null) {
                return;
            }

            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    hideLoadingMessage();
                    return;
                }
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                onSceneTapped(e);
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ARUtils.requestARInstall(this, isArInstalled);
        isArInstalled = true;

        if (arSceneView == null) {
            return;
        }

        if (arSceneView.getSession() == null) {
            try {
                final Session session = new Session(this);
                final Config config = new Config(session);

                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                session.configure(config);
                arSceneView.setupSession(session);
            } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException | UnavailableSdkTooOldException e) {
                e.printStackTrace();
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            Toast.makeText(this, "Unable to get camera", Toast.LENGTH_SHORT).show();
            finish();
        }

        arSceneView
                .getScene()
                .setOnTouchListener(
                        (HitTestResult hitTestResult, MotionEvent event) -> {
                            gestureDetector.onTouchEvent(event);
                            // Return false so touch events are also transmitted to the scene
                            return false;
                        });

        showLoadingMessage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void loadModels() {
        final CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, Uri.parse("andy.sfb")).build();
        final CompletableFuture<ModelRenderable> distillery = ModelRenderable.builder()
                .setSource(this, Uri.parse("distillery.sfb")).build();
        final CompletableFuture<ModelRenderable> hay = ModelRenderable.builder()
                .setSource(this, Uri.parse("hay.sfb")).build();
        final CompletableFuture<ViewRenderable> menuStage =
                ViewRenderable.builder().setView(this, R.layout.view_menu).build();

        CompletableFuture.allOf(
                andy,
                distillery,
                hay,
                menuStage
        ).handle((notUsed, throwable) -> {
            try {
                andyRenderable = andy.get();
                distilRenderable = distillery.get();
                hayRenderable = hay.get();
                menuRenderable = menuStage.get();

                menuRenderable.getView().findViewById(R.id.item_andy).setOnClickListener(this::onMenuItemClicked);
                menuRenderable.getView().findViewById(R.id.item_distillery).setOnClickListener(this::onMenuItemClicked);
                menuRenderable.getView().findViewById(R.id.item_hay).setOnClickListener(this::onMenuItemClicked);

                hasFinishedLoading = true;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    private void showLoadingMessage() {
        if (loadingSnackbar == null || !loadingSnackbar.isShownOrQueued()) {
            loadingSnackbar =
                    Snackbar.make(
                            this.findViewById(android.R.id.content),
                            R.string.plane_finding,
                            Snackbar.LENGTH_INDEFINITE);
            loadingSnackbar.show();
        }
    }

    private void hideLoadingMessage() {
        if (loadingSnackbar != null) {
            loadingSnackbar.dismiss();
            loadingSnackbar = null;
        }
    }

    private void onSceneTapped(MotionEvent tap) {
        if (!hasFinishedLoading) {
            return;
        }

        final Frame frame = arSceneView.getArFrame();
        if (frame != null && tap != null
                && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                final Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    displayMenu(hit.createAnchor());
                }
            }
        }
    }

    private void onMenuItemClicked(View view) {
        if (menuAnchor != null) {
            switch (view.getId()) {
                case R.id.item_distillery:
                    if (displayedObjects.containsKey(distilRenderable)) {
                        displayedObjects.get(distilRenderable).getAnchor().detach();
                    }
                    final Node distilleryNode = renderNodeForAnchor(distilRenderable, menuAnchor);
                    distilleryNode.setOnTapListener((hitTestResult, motionEvent) ->
                            animateNodeMovement(animatedNode, distilleryNode));
                    break;
                case R.id.item_hay:
                    if (displayedObjects.containsKey(hayRenderable)) {
                        displayedObjects.get(hayRenderable).getAnchor().detach();
                    }
                    final Node hayNode = renderNodeForAnchor(hayRenderable, menuAnchor);
                    hayNode.setOnTapListener((hitTestResult, motionEvent) ->
                            animateNodeMovement(animatedNode, hayNode));
                    break;
                default:
                    if (displayedObjects.containsKey(andyRenderable)) {
                        displayedObjects.get(andyRenderable).getAnchor().detach();
                    }
                    final Node andyNode = renderNodeForAnchor(andyRenderable, menuAnchor);
                    lightUpAndy(andyNode);
                    andyNode.setOnTapListener((hitTestResult, motionEvent) ->
                            animatedNode = andyNode);
                    break;
            }
            // We don't have a menu displayed anymore
            menuAnchor = null;
            menuAnchorNode.setEnabled(false);
        }
    }

    private void displayMenu(Anchor anchor) {
        // Make sure we don't have more than one Menu displayed at a time
        if (menuAnchor != null) {
            menuAnchor.detach();
        }
        // Mark that we've displayed the menu
        menuAnchor = anchor;

        if (menuAnchorNode == null) {
            // Actually render the menu
            Node node = new Node();
            node.setRenderable(menuRenderable);

            menuAnchorNode = new AnchorNode();
            menuAnchorNode.setParent(arSceneView.getScene());
            menuAnchorNode.addChild(node);

        }
        menuAnchorNode.setAnchor(anchor);
        menuAnchorNode.setEnabled(true);
    }

    private Node renderNodeForAnchor(Renderable renderable, Anchor anchor) {
        final Node node = new Node();
        node.setRenderable(renderable);

        final AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arSceneView.getScene());
        anchorNode.addChild(node);

        displayedObjects.put(renderable, anchorNode);

        return node;
    }

    private void animateNodeMovement(Node start, Node destination) {
        final ObjectAnimator movementAnimator = new ObjectAnimator();
        final Vector3 startPosition = start.getLocalPosition();
        final Vector3 endPosition = Vector3.subtract(destination.getWorldPosition(), start.getWorldPosition());

        movementAnimator.setObjectValues(startPosition, endPosition);
        movementAnimator.setPropertyName("localPosition");
        movementAnimator.setEvaluator(new Vector3Evaluator());
        movementAnimator.setInterpolator(new LinearInterpolator());
        movementAnimator.setDuration(2000);
        movementAnimator.setTarget(start);
        movementAnimator.start();
    }

    private void lightUpAndy(Node andyNode) {
        Light yellowSpotlight = Light.builder(Light.Type.SPOTLIGHT)
                .setColor(new Color(android.graphics.Color.YELLOW))
                .setShadowCastingEnabled(true)
                .build();
        andyNode.setLight(yellowSpotlight);
    }
}