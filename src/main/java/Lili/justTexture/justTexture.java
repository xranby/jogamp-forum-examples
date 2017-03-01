package Lili.justTexture;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

public class justTexture implements GLEventListener {

    private GLWindow window;
    private static GL3 gl;

    private Texture texture;
    private int[] textureId = new int[1];

    private float[] texVertexArray = new float[]{
            -1.0f, -1.0f, 0f,
            1.0f, -1.0f, 0f,
            1.0f, 1.0f, 0f,
            -1.0f, -1.0f, 0f,
            1.0f, 1.0f, 0f,
            -1.0f, 1.0f, 0f};

    private float[] texCoordArray;

    private float[] colorArray = new float[]{
            0.7f, 0.7f, 0.7f,
            0.7f, 0.7f, 0.7f,
            0.7f, 0.7f, 0.7f,
            0.7f, 0.7f, 0.7f,
            0.7f, 0.7f, 0.7f,
            0.7f, 0.7f, 0.7f};

    private int iTexVertex;
    private int iTexFragment;
    private int iTexProgram;

    private int iTexPosition;
    private int iTexColor;
    private int iTexTexture;
    private int iSampler;
    private int iTexMVP;

    private static FloatBuffer dataVertexBuffer;
    private static FloatBuffer dataColorBuffer;
    private static FloatBuffer dataTexCoordBuffer;

    private static float[] texMVP = new float[]{
            200f, 0f, 0f, 0f,
            0f, 200f, 0f, 0f,
            0f, 0f, -2.333333f, 149.99994f,
            0f, 0f, -1f, 350f};

    private static IntBuffer intBuffer;
    private static IntBuffer intVAOBuffer;

    private static int[] iVao = new int[1];


    public static void main(String[] args) {
        new justTexture();
    }

    public justTexture() {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL3));
        window = GLWindow.create(capabilities);
        window.setTitle("Just Texture");
        final Animator animator = new Animator(window);
        window.addGLEventListener(this);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });
        window.setSize(800, 800);
        window.setVisible(true);
        animator.start();
    }

    public void init(GLAutoDrawable drawable) {
        try {
            drawable.setGL(new DebugGL3(drawable.getGL().getGL3()));
            gl = drawable.getGL().getGL3();

            System.out.println("INIT GL3 IS: " + gl.getClass().getName());
            gl.setSwapInterval(1);
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            gl.glEnable(GL3.GL_DEPTH_TEST);

            try {
                URL tex = getClass().getClassLoader().getResource("main/Varna.jpg");
                texture = TextureIO.newTexture(tex, false, TextureIO.JPG);
                byte[] buf = new byte[texture.getWidth() * texture.getHeight() * 4];
                ByteBuffer buffer = ByteBuffer.wrap(buf);
                gl.glGenTextures(1, textureId, 0);
                gl.glActiveTexture(GL3.GL_TEXTURE0);
                gl.glBindTexture(GL3.GL_TEXTURE_2D, textureId[0]);

                gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, texture.getWidth(), texture.getHeight(), 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, buffer);
                texture.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
                texture.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            } catch (IOException e) {
                e.printStackTrace();
            }

            texCoordArray = new float[]{
                    texture.getImageTexCoords().left(), texture.getImageTexCoords().bottom(),
                    texture.getImageTexCoords().right(), texture.getImageTexCoords().bottom(),
                    texture.getImageTexCoords().right(), texture.getImageTexCoords().top(),
                    texture.getImageTexCoords().left(), texture.getImageTexCoords().bottom(),
                    texture.getImageTexCoords().right(), texture.getImageTexCoords().top(),
                    texture.getImageTexCoords().left(), texture.getImageTexCoords().top()
            };

            iTexVertex = initVertexShader(gl, loadShaderFile("main/colorTexture.vert"));
            iTexFragment = initFragmentShader(gl, loadShaderFile("main/colorTexture.frag"));
            iTexProgram = initShaderProgram(gl, iTexVertex, iTexFragment);

            iTexMVP = gl.glGetUniformLocation(iTexProgram, "MVP");
            iSampler = gl.glGetUniformLocation(iTexProgram, "Tex1");

            iTexPosition = gl.glGetAttribLocation(iTexProgram, "position");
            iTexColor = gl.glGetAttribLocation(iTexProgram, "color");
            iTexTexture = gl.glGetAttribLocation(iTexProgram, "texcoord");

            intVAOBuffer = Buffers.newDirectIntBuffer(1);
            gl.glGenVertexArrays(1, intVAOBuffer);

            dataVertexBuffer = Buffers.newDirectFloatBuffer(18);
            dataColorBuffer = Buffers.newDirectFloatBuffer(18);
            dataTexCoordBuffer = Buffers.newDirectFloatBuffer(12);

            for (int i = 0; i < 18; i++) {
                dataVertexBuffer.put((float) texVertexArray[i]);
                dataColorBuffer.put((float) colorArray[i]);
            }

            for (int i = 0; i < 12; i++) {
                dataTexCoordBuffer.put((float) texCoordArray[i]);
            }
            dataVertexBuffer.rewind();
            dataColorBuffer.rewind();
            dataTexCoordBuffer.rewind();

            iVao[0] = intVAOBuffer.get(0);
            gl.glBindVertexArray(iVao[0]);

            intBuffer = Buffers.newDirectIntBuffer(3);
            gl.glGenBuffers(3, intBuffer);

            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, intBuffer.get(0));
            gl.glBufferData(GL3.GL_ARRAY_BUFFER, dataVertexBuffer.capacity() * 4, dataVertexBuffer, GL3.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(iTexPosition);
            gl.glVertexAttribPointer(iTexPosition, 3, GL3.GL_FLOAT, false, 0, 0);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, intBuffer.get(1));
            gl.glBufferData(GL3.GL_ARRAY_BUFFER, dataColorBuffer.capacity() * 4, dataColorBuffer, GL3.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(iTexColor);
            gl.glVertexAttribPointer(iTexColor, 3, GL3.GL_FLOAT, false, 0, 0);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, intBuffer.get(2));
            gl.glBufferData(GL3.GL_ARRAY_BUFFER, dataTexCoordBuffer.capacity() * 4, dataTexCoordBuffer, GL3.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(iTexTexture);
            gl.glVertexAttribPointer(iTexTexture, 2, GL3.GL_FLOAT, false, 0, 0);
            gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl = drawable.getGL().getGL3();
        gl.glViewport(0, 0, width, height);
    }

    public void display(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(iTexProgram);
        gl.glUniformMatrix4fv(iTexMVP, 1, false, texMVP, 0);
        gl.glUniform1i(iSampler, 0);
        gl.glBindVertexArray(iVao[0]);
        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
        gl.glBindVertexArray(0);
    }

    public void dispose(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL3();

        gl.glDeleteProgram(iTexProgram);
        gl.glDeleteShader(iTexVertex);
        gl.glDeleteShader(iTexFragment);
    }

    private int initVertexShader(GL3 gl, String[] vp) {
        int vo = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl.glShaderSource(vo, 1, vp, null);
        gl.glCompileShader(vo);
        printShaderlog(gl, vo);
        return vo;
    }

    private int initFragmentShader(GL3 gl, String[] fp) {
        int fo = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl.glShaderSource(fo, 1, fp, null);
        gl.glCompileShader(fo);
        printShaderlog(gl, fo);
        return fo;
    }

    private int initShaderProgram(GL3 gl, int vo, int fo) {
        int po = gl.glCreateProgram();
        if (vo > 0) {
            gl.glAttachShader(po, vo);
        }
        if (fo > 0) {
            gl.glAttachShader(po, fo);
        }
        gl.glBindFragDataLocation(po, 0, "fragmentColor");
        gl.glLinkProgram(po);
        gl.glValidateProgram(po);
        printProgramlog(gl, po);
        return po;
    }

    private void printShaderlog(GL3 gl, int shader) {
        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, intBuffer);
        int infoLength = intBuffer.get(0);
        if (infoLength > 1) {
            ByteBuffer byteBuffer = Buffers.newDirectByteBuffer(infoLength);
            gl.glGetShaderInfoLog(shader, infoLength, intBuffer, byteBuffer);
            byteBuffer.rewind();
            byte dst[] = new byte[byteBuffer.capacity()];
            byteBuffer.get(dst, 0, byteBuffer.capacity());
            String message = new String(dst);
            gl.glDeleteShader(shader);
            System.out.println(message);
            throw new IllegalStateException(message);
        }
    }

    private void printProgramlog(GL3 gl, int program) {
        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(program, GL3.GL_INFO_LOG_LENGTH, intBuffer);
        int infoLength = intBuffer.get(0);
        if (infoLength > 1) {
            ByteBuffer byteBuffer = Buffers.newDirectByteBuffer(infoLength);
            gl.glGetProgramInfoLog(program, infoLength, intBuffer, byteBuffer);
            byteBuffer.rewind();
            byte dst[] = new byte[byteBuffer.capacity()];
            byteBuffer.get(dst, 0, byteBuffer.capacity());
            String message = new String(dst);
            gl.glDeleteProgram(program);
            System.out.println(message);
            throw new IllegalStateException(message);
        }
    }

    public String[] loadShaderFile(String fileName) throws IOException {
        String path = "/" + fileName;
        InputStream inputStream = path.getClass().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalStateException("Can not load shader file: " + path);
        }
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        for (String str = bufferedReader.readLine(); str != null; str = bufferedReader.readLine()) {
            stringBuffer.append(str).append('\n');
        }
        String[] result = {stringBuffer.toString()};
        inputStream.close();
        bufferedReader.close();
        return result;
    }
}
