package com.gudinna;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

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
 * JogAmp JOGL OpenGL ES 2 graph nurbs demo to expose and learn how to use the graph API to draw nurbs.
 *
 * Inside the main JOGL source tree we have the "graph" API that is what we consider the *best* way to render nurbs on all GPU's using a patent free shaders implementation.
 * Graph is suitable for both desktop and mobile GPU processors.
 *
 * In a nutshell the JogAmp Graph API enable you to define nurbs shapes
 * Outline → OutlineShapes → GLRegion
 * and then render the shapes using a Renderer
 * RegionRenderer
 * TextRenderer (same as RegionRender with Helper methods for texts and fonts.)
 *
 * outline.addVertex(x, y, z, w, onCurve);
 * outlineShape.addOutline(outline);
 * region = GLRegion.create(outlineShape, getRenderModes());
 * region.render(gl, outlineShape,...);
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

public class JogAmpGraphAPINurbsDemo {

    static Animator animator;

    public static void main(String[] args) {

        // Enable JOGL debugging of GLSL shader compilation and GL calls
        System.setProperty( "jogl.debug.GLSLCode", "");
        System.setProperty( "jogl.debug.DebugGL", "");

        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
        caps.setAlphaBits(4);
	    GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(800,400);
        glWindow.setTitle("JogAmp JOGL Graph API nurbs demo");
        glWindow.setVisible(true);

        glWindow.addGLEventListener(new GraphNurbs() /* GLEventListener */);

        animator = new Animator();
        animator.add(glWindow);
        animator.start();
    }

    private static class GraphNurbs implements GLEventListener{

        // these will define a shape that is defined once at init
        OutlineShape outlineShape;
        RenderState renderState;
        RegionRenderer regionRenderer;
        GLRegion glRegion;

        // these will define a shape that is updated dynamically for each frame
        OutlineShape dynamicOutlineShape;
        RenderState dynamicRenderState;
        RegionRenderer dynamicRegionRenderer;
        GLRegion dynamicGlRegion;

        volatile float weight = 1.0f;

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

        @Override
        public void init(GLAutoDrawable drawable) {

            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.setSwapInterval(1);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_BLEND);
            gl.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);

             /* initialize OpenGL specific classes that know how to render the graph API shapes */
            renderState = RenderState.createRenderState(SVertex.factory());
            // define a colour to render our shape with
            renderState.setColorStatic(1.0f, 1.0f, 1.0f, 1.0f);
            renderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

            /* use the generic graph API to define a shape
             * we use the renderState getVertexFactory
             * to automatically store all vertex data on the GPU
             **/
            outlineShape = new OutlineShape(renderState.getVertexFactory());

            // Here i add some points off curve causing nurbs bends
            outlineShape.addEmptyOutline();
            outlineShape.addVertex(0.0f,-10.0f, true);
            outlineShape.addVertex(17.0f,-10.0f, true);
            outlineShape.addVertex(11.0f,5.0f, /* onCurve */false);
            outlineShape.addVertex(17.0f,10.0f, true);
            outlineShape.addVertex(7.0f,15.0f, /* onCurve */ false);
            outlineShape.addVertex(6.0f,8.0f, /* onCurve */false);
            outlineShape.addVertex(0.0f,10.0f,true);
            outlineShape.closeLastOutline(true);

            // Here i add all points on curve == straight lines
            float offset = 30;
            outlineShape.addEmptyOutline();
            outlineShape.addVertex(offset+0.0f,-10.0f, true);
            outlineShape.addVertex(offset+17.0f,-10.0f, true);
            outlineShape.addVertex(offset+11.0f,5.0f, true);
            outlineShape.addVertex(offset+16.0f,10.0f, true);
            outlineShape.addVertex(offset+7.0f,15.0f, true);
            outlineShape.addVertex(offset+6.0f,8.0f, true);
            outlineShape.addVertex(offset+0.0f,10.0f, true);
            outlineShape.closeLastOutline(true);

            regionRenderer = RegionRenderer.create(renderState, /* GLCallback */ RegionRenderer.defaultBlendEnable, /* GLCallback */ RegionRenderer.defaultBlendDisable);

            glRegion = GLRegion.create(/* RenderModes */ renderModes, /* TextureSequence */ null);
            glRegion.addOutlineShape(outlineShape, null, glRegion.hasColorChannel() ? renderState.getColorStatic(new float[4]) : null);


            /* initialize OpenGL specific classes that know how to render the graph API shapes */
            dynamicRenderState = RenderState.createRenderState(SVertex.factory());
            // define a RED colour to render our shape with
            dynamicRenderState.setColorStatic(1.0f, 0.0f, 0.0f, 1.0f);
            dynamicRenderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

            dynamicRegionRenderer = RegionRenderer.create(dynamicRenderState, /* GLCallback */ RegionRenderer.defaultBlendEnable, /* GLCallback */ RegionRenderer.defaultBlendDisable);

            // we will fill the OutlineShape dynamically in display
            dynamicOutlineShape = new OutlineShape(dynamicRenderState.getVertexFactory());

            dynamicGlRegion = GLRegion.create(/* RenderModes */ renderModes, /* TextureSequence */ null);

        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            //stop the animator thread when user close the window
            animator.stop();
            // it is important to free memory allocated no the GPU!
            // this memory cant be garbage collected by the JVM
            regionRenderer.destroy(gl);
            glRegion.destroy(gl);
            dynamicGlRegion.destroy(gl);
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            // use JogAmp high resolution timer for smooth animations!
            double time = com.jogamp.common.os.Platform.currentTimeMicros();
            float sinusAnimation = (float) (Math.sin(time/100000f));
            float sinusAnimationRotate = (float) (Math.sin(time/1000000f));

            // clear screen
            gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            // the RegionRenderer PMVMatrix define where we want to render our shape
            final PMVMatrix pmv = regionRenderer.getMatrix();
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
            pmv.glTranslatef(xTranslate, yTranslate, zTranslate);
            pmv.glRotatef(angleRotate+ 10f * sinusAnimationRotate, 0, 0, 1);

            if( weight != regionRenderer.getRenderState().getWeight() ) {
                regionRenderer.getRenderState().setWeight(weight);
            }

            // Draw the static shape using RegionRenderer and GLRegion
            regionRenderer.enable(gl, true);
            glRegion.draw(gl, regionRenderer, sampleCount);
            regionRenderer.enable(gl, false);


            float offset = 60;

            // We will now update the dynamic shape that changes on each frame
            // I will animate the off curve points
            dynamicOutlineShape.clear();
            dynamicOutlineShape.addVertex(offset + 0.0f,-10.0f, true);
            dynamicOutlineShape.addVertex(offset + 17.0f,-10.0f, true);
            dynamicOutlineShape.addVertex(offset + 11.0f +5 * sinusAnimation,5.0f + 5 * sinusAnimation, /* onCurve */false);
            dynamicOutlineShape.addVertex(offset + 17.0f,10.0f, true);
            dynamicOutlineShape.addVertex(offset + 7.0f + 5 * sinusAnimation,15.0f + 5 * sinusAnimation, /* onCurve */ false);
            dynamicOutlineShape.addVertex(offset + 6.0f ,8.0f , true);
            dynamicOutlineShape.addVertex(offset + 0.0f,10.0f, true);
            dynamicOutlineShape.closeLastOutline(true);

            // the RegionRenderer PMVMatrix define where we want to render our shape
            final PMVMatrix dynamicPmv = dynamicRegionRenderer.getMatrix();
            dynamicPmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            dynamicPmv.glLoadIdentity();
            dynamicPmv.glTranslatef(xTranslate, yTranslate, zTranslate);
            dynamicPmv.glRotatef(angleRotate+ 10f * sinusAnimationRotate, 0, 0, 1);

            if( weight != dynamicRegionRenderer.getRenderState().getWeight() ) {
                dynamicRegionRenderer.getRenderState().setWeight(weight);
            }

            // when changing the OutlineShape dynamically it is very important that you clear the GPU from old data
            dynamicGlRegion.clear(gl);
            // here we upload the new dynamically created data to the GPU
            dynamicGlRegion.addOutlineShape(dynamicOutlineShape, null, glRegion.hasColorChannel() ? renderState.getColorStatic(new float[4]) : null);

             // Draw the dynamic shape using RegionRenderer and GLRegion
            dynamicRegionRenderer.enable(gl, true);
            dynamicGlRegion.draw(gl, dynamicRegionRenderer, sampleCount);
            dynamicRegionRenderer.enable(gl, false);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            final PMVMatrix pmv = regionRenderer.getMatrix();
            regionRenderer.reshapePerspective(45.0f, width, height, zNear, zFar);
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();

            final PMVMatrix dynamicPmv = dynamicRegionRenderer.getMatrix();
            dynamicRegionRenderer.reshapePerspective(45.0f, width, height, zNear, zFar);
            dynamicPmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            dynamicPmv.glLoadIdentity();
        }
    }
}
