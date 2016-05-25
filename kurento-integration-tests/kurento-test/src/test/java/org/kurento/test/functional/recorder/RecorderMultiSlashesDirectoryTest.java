/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.test.functional.recorder;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Test of a recorder, using the stream source from a WebRtcEndpoint. Tests recording with audio and
 * video. The path for recording will have several slashes in the path, at the beginning, in the
 * middle of and at the end. Also the path will have a directory that won't exist.</p> Media
 * Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li>
 * </li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording).</li>
 * <li>(Browser) First a WebRtcPeer in send-only sends media. Second, other WebRtcPeer in rcv-only
 * receives media</li>
 * </ul> Main assertion(s):
 * <ul>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.4.1
 */
public class RecorderMultiSlashesDirectoryTest extends BaseRecorder {

  private static final int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void tesRecorderMultiSlashesDirectoryWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    String multiSlashses = File.separator + File.separator + File.separator;

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile =
        getRecordUrl(extension).replace(getSimpleTestName(),
            new Date().getTime() + File.separator + getSimpleTestName());

    String recordingFileWithMultiSlashes = recordingFile.replace(File.separator, multiSlashses);

    log.info("The path with multi slash is {} ", recordingFileWithMultiSlashes);

    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, recordingFileWithMultiSlashes).withMediaProfile(
            mediaProfileSpecType).build();
    webRtcEp.connect(webRtcEp);
    webRtcEp.connect(recorderEp);

    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);
    recorderEp.record();

    // Wait until event playing in the remote stream
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    Thread.sleep(SECONDS.toMillis(PLAYTIME));

    recorderEp.stop();

    // Wait until file exists
    waitForFileExists(recordingFile);

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    mp.release();
  }

}