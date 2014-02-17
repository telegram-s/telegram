package org.telegram.android.core.video;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by ex3ndr on 17.02.14.
 */
public class CompatMuxer {
    public static void mux(VideoChunks source, String fileName) throws IOException {
        source.saveToFile(new File(fileName + ".h264"));
        H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(fileName + ".h264"));
        Movie m = new Movie();
        m.addTrack(h264Track);
        Container out = new DefaultMp4Builder().build(m);
        FileOutputStream fos = new FileOutputStream(new File(fileName));
        FileChannel fc = fos.getChannel();
        out.writeContainer(fc);
        fos.close();
    }
}
