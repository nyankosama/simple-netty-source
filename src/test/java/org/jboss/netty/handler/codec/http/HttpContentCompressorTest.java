/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.handler.codec.http;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.embedder.EncoderEmbedder;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class HttpContentCompressorTest {

    @Test
    public void testGetTargetContentEncoding() throws Exception {
        HttpContentCompressor compressor = new HttpContentCompressor();

        String[] tests = {
            // Accept-Encoding      ->     Content-Encoding
            "",                            null,
            "*",                           "gzip",
            "*;q=0.0",                     null,
            "gzip",                        "gzip",
            "compress, gzip;q=0.5",        "gzip",
            "gzip; q=0.5, identity",       "gzip",
            "gzip ; q=0.1",                "gzip",
            "gzip; q=0, deflate",          "deflate",
            " deflate ; q=0 , *;q=0.5",    "gzip",
        };
        for (int i = 0; i < tests.length; i += 2) {
            String acceptEncoding = tests[i];
            String contentEncoding = tests[i + 1];
            String targetEncoding = compressor.getTargetContentEncoding(acceptEncoding);
            assertEquals(contentEncoding, targetEncoding);
        }
    }

    static final class HttpContentCompressorEmbedder extends EncoderEmbedder<Object> {
        HttpContentCompressorEmbedder() {
            super(new HttpContentCompressor());
        }

        public void fireMessageReceived(Object msg) {
            Channels.fireMessageReceived(getChannel(), msg, null);
        }
    }

    @Test
    public void testSplitContent() throws Exception {
        HttpContentCompressorEmbedder e = new HttpContentCompressorEmbedder();
        e.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.setChunked(true);
        e.offer(res);
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("Hell", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("o, w", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("orld", CharsetUtil.US_ASCII)));
        e.offer(HttpChunk.LAST_CHUNK);

        Object o = e.poll();
        assertThat(o, is(instanceOf(HttpRequest.class)));

        o = e.poll();
        assertThat(o, is(instanceOf(HttpResponse.class)));

        res = (HttpResponse) o;
        assertThat(res.isChunked(), is(true));
        assertThat(res.getContent().readableBytes(), is(0));
        assertThat(res.headers().get(Names.TRANSFER_ENCODING), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_LENGTH), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_ENCODING), is("gzip"));

        HttpChunk chunk;
        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("1f8b0800000000000000f248cdc901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("cad7512807000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("ca2fca4901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("0300c2a99ae70c000000"));

        chunk = (HttpChunk) e.poll();
        assertThat(chunk, is(instanceOf(HttpChunkTrailer.class)));

        assertThat(e.poll(), is(nullValue()));
    }

    @Test
    public void testChunkedContent() throws Exception {
        HttpContentCompressorEmbedder e = new HttpContentCompressorEmbedder();
        e.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        e.offer(res);

        assertEncodedResponse(e);

        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("Hell", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("o, w", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("orld", CharsetUtil.US_ASCII)));
        e.offer(HttpChunk.LAST_CHUNK);

        HttpChunk chunk;
        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("1f8b0800000000000000f248cdc901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("cad7512807000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("ca2fca4901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("0300c2a99ae70c000000"));

        chunk = (HttpChunk) e.poll();
        assertThat(chunk.getContent().readable(), is(false));
        assertThat(chunk, is(instanceOf(HttpChunkTrailer.class)));

        assertThat(e.poll(), is(nullValue()));
    }

    @Test
    public void testChunkedContentWithTrailingHeader() throws Exception {
        HttpContentCompressorEmbedder e = new HttpContentCompressorEmbedder();
        e.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
        e.offer(res);

        assertEncodedResponse(e);

        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("Hell", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("o, w", CharsetUtil.US_ASCII)));
        e.offer(new DefaultHttpChunk(ChannelBuffers.copiedBuffer("orld", CharsetUtil.US_ASCII)));
        HttpChunkTrailer trailer = new DefaultHttpChunkTrailer();
        trailer.trailingHeaders().set("X-Test", "Netty");
        e.offer(trailer);

        HttpChunk chunk;
        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("1f8b0800000000000000f248cdc901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("cad7512807000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("ca2fca4901000000ffff"));

        chunk = (HttpChunk) e.poll();
        assertThat(ChannelBuffers.hexDump(chunk.getContent()), is("0300c2a99ae70c000000"));
        assertThat(chunk, is(instanceOf(HttpChunk.class)));

        chunk = (HttpChunk) e.poll();
        assertThat(chunk.getContent().readable(), is(false));
        assertThat(chunk, is(instanceOf(HttpChunkTrailer.class)));
        assertEquals("Netty", ((HttpChunkTrailer) chunk).trailingHeaders().get("X-Test"));

        assertThat(e.poll(), is(nullValue()));
    }

    @Test
    public void testFullContent() throws Exception {
        HttpContentCompressorEmbedder e = new HttpContentCompressorEmbedder();
        e.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.setContent(ChannelBuffers.copiedBuffer("Hello, World", CharsetUtil.US_ASCII));
        res.headers().set(Names.CONTENT_LENGTH, res.getContent().readableBytes());
        e.offer(res);

        Object o = e.poll();
        assertThat(o, is(instanceOf(HttpRequest.class)));

        o = e.poll();
        assertThat(o, is(instanceOf(HttpResponse.class)));

        res = (HttpResponse) o;
        assertThat(res.isChunked(), is(false));
        assertThat(
                ChannelBuffers.hexDump(res.getContent()),
                is("1f8b0800000000000000f248cdc9c9d75108cf2fca4901000000ffff0300c6865b260c000000"));
        assertThat(res.headers().get(Names.TRANSFER_ENCODING), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_LENGTH), is(String.valueOf(res.getContent().readableBytes())));
        assertThat(res.headers().get(Names.CONTENT_ENCODING), is("gzip"));

        assertThat(e.poll(), is(nullValue()));
    }

    /**
     * If the length of the content is unknown, {@link HttpContentEncoder} should not skip encoding the content
     * even if the actual length is turned out to be 0.
     */
    @Test
    public void testEmptySplitContent() throws Exception {
        HttpContentCompressorEmbedder e = new HttpContentCompressorEmbedder();
        e.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.setChunked(true);
        e.offer(res);

        Object o = e.poll();
        assertThat(o, is(instanceOf(HttpRequest.class)));

        o = e.poll();
        assertThat(o, is(instanceOf(HttpResponse.class)));

        res = (HttpResponse) o;
        assertThat(res.getContent().readableBytes(), is(0));
        assertThat(res.isChunked(), is(true));
        assertThat(res.headers().get(Names.TRANSFER_ENCODING), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_LENGTH), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_ENCODING), is("gzip"));

        e.offer(HttpChunk.LAST_CHUNK);

        HttpChunk chunk = (HttpChunk) e.poll();
        assertThat(chunk.getContent().readableBytes(), is(20)); // an empty gzip stream is 20 bytes long.
        assertThat(chunk, is(instanceOf(HttpChunk.class)));

        chunk = (HttpChunk) e.poll();
        assertThat(chunk.getContent().readable(), is(false));
        assertThat(chunk, is(instanceOf(HttpChunkTrailer.class)));

        assertThat(e.poll(), is(nullValue()));
    }

    /**
     * If the length of the content is 0 for sure, {@link HttpContentEncoder} should skip encoding.
     */
    @Test
    public void testEmptyFullContent() throws Exception {
        HttpContentCompressorEmbedder ch = new HttpContentCompressorEmbedder();
        ch.fireMessageReceived(newRequest());

        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        res.setContent(ChannelBuffers.EMPTY_BUFFER);
        ch.offer(res);

        Object o = ch.poll();
        assertThat(o, is(instanceOf(HttpRequest.class)));

        o = ch.poll();
        assertThat(o, is(instanceOf(HttpResponse.class)));

        res = (HttpResponse) o;
        assertThat(res.headers().get(Names.TRANSFER_ENCODING), is(nullValue()));

        // Content encoding shouldn't be modified.
        assertThat(res.headers().get(Names.CONTENT_ENCODING), is(nullValue()));
        assertThat(res.getContent().readableBytes(), is(0));
        assertThat(res.getContent().toString(CharsetUtil.US_ASCII), is(""));

        assertThat(ch.poll(), is(nullValue()));
    }

    private static HttpRequest newRequest() {
        HttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        req.headers().set(Names.ACCEPT_ENCODING, "gzip");
        return req;
    }

    private static void assertEncodedResponse(HttpContentCompressorEmbedder e) {
        Object o = e.poll();
        assertThat(o, is(instanceOf(HttpRequest.class)));

        o = e.poll();
        assertThat(o, is(instanceOf(HttpResponse.class)));

        HttpResponse res = (HttpResponse) o;
        assertThat(res.getContent().readableBytes(), is(0));
        assertThat(res.headers().get(Names.TRANSFER_ENCODING), is("chunked"));
        assertThat(res.headers().get(Names.CONTENT_LENGTH), is(nullValue()));
        assertThat(res.headers().get(Names.CONTENT_ENCODING), is("gzip"));
    }
}
