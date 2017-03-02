package xranby.com.gudinna;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

import java.io.IOException;

/**
 * <pre>
 *   __ __|_  ___________________________________________________________________________  ___|__ __
 *  //    /\                                           _                                  /\    \\
 * //____/  \__     __ _____ _____ _____ _____ _____  | |     __ _____ _____ __        __/  \____\\
 *  \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /
 *   \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/
 *  /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\
 * /  \____\                       http://jogamp.org  |_|                              /____/  \
 * \  /   "' _________________________________________________________________________ `"   \  /
 *  \/____.                                                                             .____\/
 * </pre>
 *
 * <p>
 * JogAmp JOGL OpenGL ES 2 graph text demo to expose and learn how to use the graph API to draw text.
 *
 * Inside the main JOGL source tree we have the "Graph" API that is what we consider
 * the *best* way to render text using nurbs on all GPU's.
 * Graph is using a patent free shaders implementation.
 * Graph is suitable for both desktop and mobile GPU processors.
 *
 * NOTE: This demo is using jogamp.graph.font.fonts.ubuntu is found inside jogl-fonts-p0.jar
 * you may need to add this jar to your classpath
 * http://jogamp.org/deployment/jogamp-current/jar/atomic/jogl-fonts-p0.jar
 *
 * In a nutshell the JogAmp Graph API enable you to define nurbs shapes
 * Outline → OutlineShapes → GLRegion
 * and then render the shapes using a Renderer
 * RegionRenderer
 * TextRegionUtil (same as RegionRender with Helper methods for texts and fonts.)
 *
 * To load a Font you need to implement your own FontSet
 * The JogAmp JOGL source tree contains two FontSet's
 * One for loading Ubuntu true type fonts bundled with JogAmp
 * One for loading "Java" true type fonts bundled with the JRE
 * http://jogamp.org/git/?p=jogl.git;a=blob;f=src/jogl/classes/jogamp/graph/font/UbuntuFontLoader.java;hb=HEAD
 * http://jogamp.org/git/?p=jogl.git;a=blob;f=src/jogl/classes/jogamp/graph/font/JavaFontLoader.java;hb=HEAD
 * The FontFactory class can be used to load a default JogAmp FontSet Font.
 *
 * The graph API is using the math by Rami Santina introduced in 2011
 * https://jogamp.org/doc/gpunurbs2011/p70-santina.pdf
 * https://jogamp.org/doc/gpunurbs2011/graphicon2011-slides.pdf
 *
 * The best documentation for the graph API is found in the JOGL junit tests
 * http://jogamp.org/git/?p=jogl.git;a=tree;f=src/test/com/jogamp/opengl/test/junit/graph;hb=HEAD
 *
 * and javadoc for Outline and OutlineShape .. and all classes i mentioned above..
 * https://www.jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/geom/Outline.html
 * https://www.jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/curve/OutlineShape.html
 * </p>
 *
 * @author Xerxes Rånby (xranby)
 */

public class JogAmpGraphAPITextDemo {

    static Animator animator;

    public static void main(String[] args) {

        // Enable JOGL debugging of GLSL shader compilation and GL calls
        //System.setProperty( "jogl.debug.GLSLCode", "");
        //System.setProperty( "jogl.debug.DebugGL", "");

        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
        caps.setAlphaBits(4);
        GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(800,400);
        glWindow.setTitle("JogAmp JOGL Graph API text demo");
        glWindow.setVisible(true);

        glWindow.addGLEventListener(new GraphText() /* GLEventListener */);

        animator = new Animator();
        animator.add(glWindow);
        animator.start();

        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                new Thread(new Runnable() {
                    public void run() {
                        //stop the animator thread when user close the window
                        animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });
    }

    private static class GraphText implements GLEventListener{

        TextRegionUtil textRegionUtil;
        RenderState renderState;
        RegionRenderer regionRenderer;

        Font font;
        static int fontSet = FontFactory.UBUNTU;
        static int fontFamily = 0; // default
        static int fontStyleBits = 0; // default

        volatile float weight = 1.0f;
        volatile double lastTime = com.jogamp.common.os.Platform.currentTimeMicros();

        final float fontSize = 10.0f;

        final float zNear = 0.1f, zFar = 7000f;

        /* 2nd pass texture size antialias SampleCount
           4 is usually enough */
        private final int[] sampleCount = new int[] { 4 };

        /* variables used to update the PMVMatrix before rendering */
        private float xTranslate = -40f;
        private float yTranslate =  0f;
        private float zTranslate = -100f;
        private float angleRotate = 0f;

        private final int renderModes = Region.VARWEIGHT_RENDERING_BIT;

        double fpsHuman;

        @Override
        public void init(GLAutoDrawable drawable) {

            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            /* SwapInterval 1 makes the demo run at 60 fps
            use SwapInterval 0 here to get ... hundreds ... sometimes thousands of fps!
            SwapInterval 0 can cause stuttering due to thermal throttling of your GPU */
            gl.setSwapInterval(1);

            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_BLEND);
            gl.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);

            /* load a ttf font */
            try {
                /* JogAmp FontFactory will load a true type font
                 *
                 * fontSet == 0 loads
                 * jogamp.graph.font.fonts.ubuntu found inside jogl-fonts-p0.jar
                 * http://jogamp.org/deployment/jogamp-current/jar/atomic/jogl-fonts-p0.jar
                 *
                 * fontSet == 1 tries loads LucidaBrightRegular from the JRE.
                 */

                font = FontFactory.get(fontSet).get(fontFamily, fontStyleBits);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }


            /* initialize OpenGL specific classes that know how to render the graph API shapes */
            renderState = RenderState.createRenderState(SVertex.factory());
            // define a RED colour to render our shape with
            renderState.setColorStatic(1.0f, 0.0f, 0.0f, 1.0f);
            renderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

            regionRenderer = RegionRenderer.create(renderState, /* GLCallback */ RegionRenderer.defaultBlendEnable, /* GLCallback */ RegionRenderer.defaultBlendDisable);

            textRegionUtil = new TextRegionUtil(renderModes);

            regionRenderer.init(gl, renderModes);
            regionRenderer.enable(gl, false);
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            // it is important to free memory allocated no the GPU!
            // this memory cant be garbage collected by the JVM
            renderState.destroy(gl);
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            // use JogAmp high resolution timer for smooth animations!
            double time = com.jogamp.common.os.Platform.currentTimeMicros();
            double fps = Math.floor(1000000f/(time-lastTime));
            lastTime = time;

            // fps updates too fast for humans to read... sample an fps at some frames.
            if(time%100==0) {
                fpsHuman = fps;
            }

            float sinusAnimationRotate = (float) (Math.sin(time/1000000f));
            float sinusAnimationJump = (float) (Math.sin(time/100000f));

            String text = "JogAmp GRAPH API Text demo\nFPS: "+fps+"\nFPS human readable: "+fpsHuman;

            // clear screen
            gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            float offsetX = -30;
            float offsetY = 30;

            // When rendering text we need to account for newlines inside the text.
            final int newLineCount = TextRegionUtil.getCharCount(text, '\n');

            final float lineHeight = font.getLineHeight(fontSize);
            offsetX += font.getAdvanceWidth('X', fontSize);
            offsetY -= lineHeight * newLineCount;

            // the RegionRenderer PMVMatrix define where we want to render our shape
            final PMVMatrix Pmv = regionRenderer.getMatrix();
            Pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            Pmv.glLoadIdentity();
            Pmv.glTranslatef(xTranslate+offsetX, yTranslate+offsetY, zTranslate+(sinusAnimationJump*10f));
            Pmv.glRotatef(angleRotate+ 10f * sinusAnimationRotate, 0, 0, 1);

            if( weight != regionRenderer.getRenderState().getWeight() ) {
                regionRenderer.getRenderState().setWeight(weight);
            }

            // Draw the  shape using RegionRenderer and TextRegionUtil
            regionRenderer.enable(gl, true);
            textRegionUtil.drawString3D(gl, regionRenderer, font, fontSize, text, null, sampleCount);
            regionRenderer.enable(gl, false);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            final PMVMatrix Pmv = regionRenderer.getMatrix();
            regionRenderer.reshapePerspective(45.0f, width, height, zNear, zFar);
            Pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            Pmv.glLoadIdentity();
        }
    }
}
