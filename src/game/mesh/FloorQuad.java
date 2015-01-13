package game.mesh;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

public class FloorQuad extends Mesh {

    private int width;
    private int height;

    public FloorQuad(int width, int height) {
        updateGeometry(width, height);
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public void updateGeometry(int width, int height) {
        this.width = width;
        this.height = height;

        Vector3f[] vertices = new Vector3f[(width + 1) * (height + 1)];
        Vector2f[] texCoord = new Vector2f[(width + 1) * (height + 1)];
        int[] indexes = new int[6 * width * height];
        float[] normals = new float[3 * (width + 1) * (height + 1)];

        int i = 0;
        int j = 0;
        int k = 0;
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                vertices[i] = new Vector3f(x, y, 0);
                texCoord[i] = new Vector2f((float) x / width, (float) y
                        / height);

                normals[k++] = 0;
                normals[k++] = 0;
                normals[k++] = 1;

                if (x != width && y != height) {
                    indexes[j++] = i;
                    indexes[j++] = i + height + 1;
                    indexes[j++] = i + height + 2;
                    indexes[j++] = i;
                    indexes[j++] = i + height + 2;
                    indexes[j++] = i + 1;
                }
                i++;
            }
        }

        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        updateBound();
    }
}