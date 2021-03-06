package terrains;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

public class Terrain {

	public static final int NUM_HEXAGONS_X = 10;
	public static final int NUM_HEXAGONS_Z = 10;

	public static final float HEX_SIDE = 1f;
	public static final float HEX_HALF_SIDE = HEX_SIDE / 2.0f;
	public static final float HEX_SQRT3 = HEX_SIDE * (float) Math.sqrt(3);
	public static final float HEX_HALF_SQRT3 = HEX_SQRT3 / 2.0f;
	public static final float HEX_MIN_TRI_AREA = HEX_SIDE * HEX_HALF_SQRT3;
	public static int levelOfDetail = 1;

	public static final float X_SIZE = NUM_HEXAGONS_X * getHexSqrt3();
	public static final float Z_SIZE = 1.5f * NUM_HEXAGONS_Z * getHexSide();

	// Number of indices constructing the hexagons
	public static final int NUM_INDICES_CONTRUCTING_HEX = NUM_HEXAGONS_X * NUM_HEXAGONS_Z * 12;
	// Number of vertices until reaching the next row of hexagon
	private static final int DIFF_NEXT_ROW = NUM_HEXAGONS_X * 6;

	private float x;
	private float z;
	private int gridX;
	private int gridZ;
	private RawModel model;
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;

	public float[][] heights;

	public Terrain(int gridX, int gridZ, TerrainTexturePack texture, TerrainTexture blendMap, String heightMap) {
		texturePack = texture;
		this.blendMap = blendMap;
		x = gridX * X_SIZE;
		z = gridZ * Z_SIZE;
		this.gridX = gridX;
		this.gridZ = gridZ;
		model = generateHexagonMeshTerrain(heightMap);
	}

	public float getX() {
		return x;
	}

	public float getZ() {
		return z;
	}

	public RawModel getModel() {
		return model;
	}

	public TerrainTexturePack getTexturePack() {
		return texturePack;
	}

	public TerrainTexture getBlendMap() {
		return blendMap;
	}

	private Vector3f calculateHexagonMeshNormal(Vector3f center, Vector3f vertice) {
		Vector3f centerToPoint = null;
		centerToPoint = Vector3f.sub(vertice, center, null);
		centerToPoint.normalise();
		Vector3f normalVector = null;
		normalVector = Vector3f.add(centerToPoint, new Vector3f(0, 6, 0), null);
		normalVector.normalise();
		return normalVector;
	}

	private RawModel generateHexagonMeshTerrain(String heightMap) {
		BufferedReader reader = null;
		String[] data;
		try {
			reader = new BufferedReader(new FileReader("res/" + heightMap + ".txt"));
		} catch (IOException e) {
			System.out.println("Failed to read any data from height map at res/" + heightMap + ".txt");
			e.printStackTrace();
			return null;
		}
		heights = new float[NUM_HEXAGONS_Z][NUM_HEXAGONS_X];
		int count = NUM_HEXAGONS_X * NUM_HEXAGONS_Z;
		float[] vertices = new float[(int) (count * 18)];
		float[] normals = new float[(int) (count * 18)];
		float[] textureCoords = new float[(int) (count * 12)];
		try {
			String line;
			int rowNumber = 0;
			boolean isOffsetFromLeft = false;
			float terrainSizeX = (NUM_HEXAGONS_X + 0.5f) * getHexSqrt3();
			float terrainSizeY = (NUM_HEXAGONS_Z * 1.5f + 0.5f) * getHexSide();
			for (int i = 0; i < gridZ * NUM_HEXAGONS_Z; i++) {
				reader.readLine();
			}
			while (rowNumber < NUM_HEXAGONS_Z && (line = reader.readLine()) != null) {
				data = line.split(",");
				for (int columnNumber = 0; columnNumber < NUM_HEXAGONS_X; columnNumber++) {
//					float height = Float.parseFloat(data[columnNumber]);
					float height = Float.parseFloat(data[columnNumber + gridX * NUM_HEXAGONS_X]);
					heights[rowNumber][columnNumber] = height;
					float referencePointX = getHexSqrt3() * (columnNumber + (isOffsetFromLeft ? 0.5f : 0));
					float referencePointZ = 1.5f * getHexSide() * rowNumber;
					int startingVerticeIndex = 18 * (rowNumber * NUM_HEXAGONS_X + columnNumber);
					vertices[startingVerticeIndex] = referencePointX + getHexHalfSqrt3();
					vertices[startingVerticeIndex + 2] = referencePointZ;
					vertices[startingVerticeIndex + 3] = referencePointX;
					vertices[startingVerticeIndex + 5] = referencePointZ + 0.5f * getHexSide();
					vertices[startingVerticeIndex + 6] = referencePointX;
					vertices[startingVerticeIndex + 8] = referencePointZ + 1.5f * getHexSide();
					vertices[startingVerticeIndex + 9] = referencePointX + getHexHalfSqrt3();
					vertices[startingVerticeIndex + 11] = referencePointZ + 2 * getHexSide();
					vertices[startingVerticeIndex + 12] = referencePointX + getHexSqrt3();
					vertices[startingVerticeIndex + 14] = referencePointZ + 1.5f * getHexSide();
					vertices[startingVerticeIndex + 15] = referencePointX + getHexSqrt3();
					vertices[startingVerticeIndex + 17] = referencePointZ + 0.5f * getHexSide();
					for (int i = 0; i < 6; i++) {
						vertices[startingVerticeIndex + 1 + i * 3] = height;
					}
					Vector3f center = new Vector3f(referencePointX + getHexHalfSqrt3(), height, referencePointZ + getHexSide());
					for (int i = 0; i < 6; i++) {
						Vector3f vertice = new Vector3f(vertices[startingVerticeIndex + i * 3], height, vertices[startingVerticeIndex + i * 3 + 2]);
						Vector3f normal = calculateHexagonMeshNormal(center, vertice);
//						normal = new Vector3f(0, 1, 0);
						normals[startingVerticeIndex + i * 3] = normal.x;
						normals[startingVerticeIndex + i * 3 + 1] = normal.y;
						normals[startingVerticeIndex + i * 3 + 2] = normal.z;
					}
					int startingTextureIndex = 12 * (rowNumber * NUM_HEXAGONS_X + columnNumber);
					for (int i = 0; i < 6; i++) {
						textureCoords[startingTextureIndex + i * 2] = vertices[startingVerticeIndex + i * 3] / terrainSizeX;
						textureCoords[startingTextureIndex + i * 2 + 1] = vertices[startingVerticeIndex + i * 3 + 2] / terrainSizeY;
					}
				}
				rowNumber++;
				isOffsetFromLeft = !isOffsetFromLeft;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int hexSideIndicesCount = 6 * (3 * (NUM_HEXAGONS_X - 1) * (NUM_HEXAGONS_Z - 1) + NUM_HEXAGONS_X + NUM_HEXAGONS_Z - 2);
		int[] indices = new int[12 * count + hexSideIndicesCount];
		int startingIndiceIndex = 0;
		int startingVerticeIndex = 0;
		int[] hexIndiceArray = new int[] { 0, 2, 4, 0, 1, 2, 2, 3, 4, 0, 4, 5 };
		for (int y = 0; y < NUM_HEXAGONS_Z; y++) {
			for (int x = 0; x < NUM_HEXAGONS_X; x++) {
				for (int i = 0; i < 12; i++) {
					indices[startingIndiceIndex++] = startingVerticeIndex + hexIndiceArray[i];
				}
				startingVerticeIndex += 6;
			}
		}
		startingVerticeIndex = 0;
		int[] hexSideIndiceArrayNonOffset = new int[] { 4, DIFF_NEXT_ROW, DIFF_NEXT_ROW + 1, 4, 3, DIFF_NEXT_ROW + 1, 8, 4, 5, 8, 7, 5, DIFF_NEXT_ROW, 8, 9, DIFF_NEXT_ROW, DIFF_NEXT_ROW + 5, 9 };
		int[] hexSideIndiceArrayOffset = new int[] { 4, DIFF_NEXT_ROW + 6, DIFF_NEXT_ROW + 7, 4, 3, DIFF_NEXT_ROW + 7, 8, 4, 5, 8, 7, 5, DIFF_NEXT_ROW + 6, 8, 9, DIFF_NEXT_ROW + 6, DIFF_NEXT_ROW + 11, 9 };
		boolean offset = false;
		for (int y = 0; y < NUM_HEXAGONS_Z - 1; y++) {
//		for (int y = 0; y < 1; y++) {
			int[] useArray = offset ? hexSideIndiceArrayOffset : hexSideIndiceArrayNonOffset;
			for (int x = 0; x < NUM_HEXAGONS_X - 1; x++) {
				for (int i = 0; i < 18; i++) {
					indices[startingIndiceIndex++] = startingVerticeIndex + useArray[i];
				}
				startingVerticeIndex += 6;
			}
			offset = !offset;
			startingVerticeIndex += 6;
		}
		return Loader.loadToVAO(vertices, textureCoords, normals, indices);
	}

//	private RawModel generateHexagonMeshTerrain(TerrainData terrainData, int terrainX, int terrainZ) {
//		Hexagon[][] hexagons = new float[NUM_HEXAGONS_Z][NUM_HEXAGONS_X];
//		int count = NUM_HEXAGONS_X * NUM_HEXAGONS_Z;
//		float[] vertices = new float[(int) (count * 18)];
//		float[] normals = new float[(int) (count * 18)];
//		float[] textureCoords = new float[(int) (count * 12)];
//		try {
//			String line;
//			int rowNumber = 0;
//			boolean isOffsetFromLeft = false;
//			float terrainSizeX = (NUM_HEXAGONS_X + 0.5f) * getHexSqrt3();
//			float terrainSizeY = (NUM_HEXAGONS_Z * 1.5f + 0.5f) * getHexSide();
//			for (int i = 0; i < gridZ * NUM_HEXAGONS_Z; i++) {
//				reader.readLine();
//			}
//			while (rowNumber < NUM_HEXAGONS_Z && (line = reader.readLine()) != null) {
//				data = line.split(",");
//				for (int columnNumber = 0; columnNumber < NUM_HEXAGONS_X; columnNumber++) {
////					float height = Float.parseFloat(data[columnNumber]);
//					float height = Float.parseFloat(data[columnNumber + gridX * NUM_HEXAGONS_X]);
//					heights[rowNumber][columnNumber] = height;
//					float referencePointX = getHexSqrt3() * (columnNumber + (isOffsetFromLeft ? 0.5f : 0));
//					float referencePointZ = 1.5f * getHexSide() * rowNumber;
//					int startingVerticeIndex = 18 * (rowNumber * NUM_HEXAGONS_X + columnNumber);
//					vertices[startingVerticeIndex] = referencePointX + getHexHalfSqrt3();
//					vertices[startingVerticeIndex + 2] = referencePointZ;
//					vertices[startingVerticeIndex + 3] = referencePointX;
//					vertices[startingVerticeIndex + 5] = referencePointZ + 0.5f * getHexSide();
//					vertices[startingVerticeIndex + 6] = referencePointX;
//					vertices[startingVerticeIndex + 8] = referencePointZ + 1.5f * getHexSide();
//					vertices[startingVerticeIndex + 9] = referencePointX + getHexHalfSqrt3();
//					vertices[startingVerticeIndex + 11] = referencePointZ + 2 * getHexSide();
//					vertices[startingVerticeIndex + 12] = referencePointX + getHexSqrt3();
//					vertices[startingVerticeIndex + 14] = referencePointZ + 1.5f * getHexSide();
//					vertices[startingVerticeIndex + 15] = referencePointX + getHexSqrt3();
//					vertices[startingVerticeIndex + 17] = referencePointZ + 0.5f * getHexSide();
//					for (int i = 0; i < 6; i++) {
//						vertices[startingVerticeIndex + 1 + i * 3] = height;
//					}
//					Vector3f center = new Vector3f(referencePointX + getHexHalfSqrt3(), height, referencePointZ + getHexSide());
//					for (int i = 0; i < 6; i++) {
//						Vector3f vertice = new Vector3f(vertices[startingVerticeIndex + i * 3], height, vertices[startingVerticeIndex + i * 3 + 2]);
//						Vector3f normal = calculateHexagonMeshNormal(center, vertice);
////						normal = new Vector3f(0, 1, 0);
//						normals[startingVerticeIndex + i * 3] = normal.x;
//						normals[startingVerticeIndex + i * 3 + 1] = normal.y;
//						normals[startingVerticeIndex + i * 3 + 2] = normal.z;
//					}
//					int startingTextureIndex = 12 * (rowNumber * NUM_HEXAGONS_X + columnNumber);
//					for (int i = 0; i < 6; i++) {
//						textureCoords[startingTextureIndex + i * 2] = vertices[startingVerticeIndex + i * 3] / terrainSizeX;
//						textureCoords[startingTextureIndex + i * 2 + 1] = vertices[startingVerticeIndex + i * 3 + 2] / terrainSizeY;
//					}
//				}
//				rowNumber++;
//				isOffsetFromLeft = !isOffsetFromLeft;
//			}
//			reader.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		int hexSideIndicesCount = 6 * (3 * (NUM_HEXAGONS_X - 1) * (NUM_HEXAGONS_Z - 1) + NUM_HEXAGONS_X + NUM_HEXAGONS_Z - 2);
//		int[] indices = new int[12 * count + hexSideIndicesCount];
//		int startingIndiceIndex = 0;
//		int startingVerticeIndex = 0;
//		int[] hexIndiceArray = new int[] { 0, 2, 4, 0, 1, 2, 2, 3, 4, 0, 4, 5 };
//		for (int y = 0; y < NUM_HEXAGONS_Z; y++) {
//			for (int x = 0; x < NUM_HEXAGONS_X; x++) {
//				for (int i = 0; i < 12; i++) {
//					indices[startingIndiceIndex++] = startingVerticeIndex + hexIndiceArray[i];
//				}
//				startingVerticeIndex += 6;
//			}
//		}
//		startingVerticeIndex = 0;
//		int[] hexSideIndiceArrayNonOffset = new int[] { 4, DIFF_NEXT_ROW, DIFF_NEXT_ROW + 1, 4, 3, DIFF_NEXT_ROW + 1, 8, 4, 5, 8, 7, 5, DIFF_NEXT_ROW, 8, 9, DIFF_NEXT_ROW, DIFF_NEXT_ROW + 5, 9 };
//		int[] hexSideIndiceArrayOffset = new int[] { 4, DIFF_NEXT_ROW + 6, DIFF_NEXT_ROW + 7, 4, 3, DIFF_NEXT_ROW + 7, 8, 4, 5, 8, 7, 5, DIFF_NEXT_ROW + 6, 8, 9, DIFF_NEXT_ROW + 6, DIFF_NEXT_ROW + 11, 9 };
//		boolean offset = false;
//		for (int y = 0; y < NUM_HEXAGONS_Z - 1; y++) {
////		for (int y = 0; y < 1; y++) {
//			int[] useArray = offset ? hexSideIndiceArrayOffset : hexSideIndiceArrayNonOffset;
//			for (int x = 0; x < NUM_HEXAGONS_X - 1; x++) {
//				for (int i = 0; i < 18; i++) {
//					indices[startingIndiceIndex++] = startingVerticeIndex + useArray[i];
//				}
//				startingVerticeIndex += 6;
//			}
//			offset = !offset;
//			startingVerticeIndex += 6;
//		}
//		return Loader.loadToVAO(vertices, textureCoords, normals, indices);
//	}

	public static float getHexSide() {
		return HEX_SIDE * levelOfDetail;
	}

	public static float getHexHalfSide() {
		return HEX_HALF_SIDE * levelOfDetail;
	}

	public static float getHexSqrt3() {
		return HEX_SQRT3 * levelOfDetail;
	}

	public static float getHexHalfSqrt3() {
		return HEX_HALF_SQRT3 * levelOfDetail;
	}

	public static float getHexMinTriArea() {
		return HEX_MIN_TRI_AREA * levelOfDetail;
	}
}
