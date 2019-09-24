/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.extensions;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Pair;
import android.util.Size;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class ImageCaptureExtenderTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FakeLifecycleOwner mLifecycleOwner;
    private ImageCaptureExtenderImpl mMockImageCaptureExtenderImpl;
    private ArrayList<CaptureStageImpl> mCaptureStages = new ArrayList<>(); {
        mCaptureStages.add(new FakeCaptureStage());
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        mLifecycleOwner = new FakeLifecycleOwner();
        mMockImageCaptureExtenderImpl = mock(ImageCaptureExtenderImpl.class);

        when(mMockImageCaptureExtenderImpl.getCaptureStages()).thenReturn(mCaptureStages);

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.init(context, Camera2AppConfig.create(context));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        // Wait for CameraX to finish deinitializing before the next test.
        CameraX.deinit().get();
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreGetCaptureStagesBeforeAndAfterInitDeInit() {

        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mMockImageCaptureExtenderImpl, null);
        ImageCaptureConfig.Builder configBuilder =
                new ImageCaptureConfig.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventListener(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class));

        ImageCapture useCase = new ImageCapture(configBuilder.build());
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycleOwner, useCase);
                mLifecycleOwner.startAndResume();
            }
        });

        // To verify the event callbacks in order, and to verification of the getCaptureStages()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mMockImageCaptureExtenderImpl);
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).getCaptureStages();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Unbind the use case to test the onDeInit.
                CameraX.unbind(useCase);
            }
        });

        // To verify the deInit should been called.
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mMockImageCaptureExtenderImpl);
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreCameraEventCallbacksBeforeAndAfterInitDeInit() {

        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mMockImageCaptureExtenderImpl, null);
        ImageCaptureConfig.Builder configBuilder =
                new ImageCaptureConfig.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventListener(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class));
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureAdapter));

        ImageCapture useCase = new ImageCapture(configBuilder.build());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycleOwner, useCase);
                mLifecycleOwner.startAndResume();
            }
        });

        // To verify the event callbacks in order, and to verification of the onEnableSession()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mMockImageCaptureExtenderImpl);
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onPresetSession();
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onEnableSession();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Unbind the use case to test the onDisableSession and onDeInit.
                CameraX.unbind(useCase);
            }
        });

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mMockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onDisableSession();
        inOrder.verify(mMockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // This test item only focus on onPreset, onEnable and onDisable callback testing,
        // ignore all the getCaptureStages callbacks.
        verify(mMockImageCaptureExtenderImpl, atLeastOnce()).getCaptureStages();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mMockImageCaptureExtenderImpl);
    }

    @Test
    @SmallTest
    public void canSetSupportedResolutionsToConfigTest()
            throws CameraInfoUnavailableException, CameraAccessException {
        CameraX.LensFacing lensFacing = CameraX.LensFacing.BACK;
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing));
        assumeTrue(ExtensionsManager.isExtensionAvailable(EffectMode.BEAUTY, lensFacing));
        ImageCaptureConfig.Builder configBuilder = new ImageCaptureConfig.Builder().setLensFacing(
                lensFacing);

        String cameraId = androidx.camera.extensions.CameraUtil.getCameraId(
                ((CameraDeviceConfig) configBuilder.build()));
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(
                        CameraX.getCameraWithLensFacing(lensFacing));

        // Only BeautyImageCaptureExtenderImpl has sample implementation for ImageCapture.
        // Retrieves the target format/resolutions pair list directly from
        // BeautyImageCaptureExtenderImpl.
        BeautyImageCaptureExtenderImpl impl = new BeautyImageCaptureExtenderImpl();

        impl.init(cameraId, cameraCharacteristics);
        List<Pair<Integer, Size[]>> targetFormatResolutionsPairList =
                impl.getSupportedResolutions();

        assertThat(targetFormatResolutionsPairList).isNotNull();

        // Retrieves the target format/resolutions pair list from builder after applying beauty
        // mode.
        BeautyImageCaptureExtender extender = BeautyImageCaptureExtender.create(configBuilder);
        assertThat(configBuilder.build().getSupportedResolutions(null)).isNull();
        extender.enableExtension();

        List<Pair<Integer, Size[]>> resultFormatResolutionsPairList =
                configBuilder.build().getSupportedResolutions(null);

        assertThat(resultFormatResolutionsPairList).isNotNull();

        // Checks the result and target pair lists are the same
        for (Pair<Integer, Size[]> resultPair : resultFormatResolutionsPairList) {
            Size[] targetSizes = null;
            for (Pair<Integer, Size[]> targetPair : targetFormatResolutionsPairList) {
                if (targetPair.first.equals(resultPair.first)) {
                    targetSizes = targetPair.second;
                    break;
                }
            }

            assertThat(Arrays.asList(resultPair.second).equals(
                    Arrays.asList(targetSizes))).isTrue();
        }
    }

    final class FakeCaptureStage implements CaptureStageImpl {

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public List<Pair<CaptureRequest.Key, Object>> getParameters() {
            List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
            return parameters;
        }
    }
}
