package jadx.core.clsp;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classes list for import into classpath graph
 */
public class ClsSet {
	private static final Logger LOG = LoggerFactory.getLogger(ClsSet.class);

	private static final String CLST_EXTENSION = ".jcst";
	private static final String CLST_FILENAME = "core" + CLST_EXTENSION;
	private static final String CLST_PKG_PATH = ClsSet.class.getPackage().getName().replace('.', '/');

	private static final String JADX_CLS_SET_HEADER = "jadx-cst";
	private static final int VERSION = 1;

	private static final String STRING_CHARSET = "US-ASCII";

	private NClass[] classes;

	public void load(RootNode root) {
		List<ClassNode> list = root.getClasses(true);
		Map<String, NClass> names = new HashMap<String, NClass>(list.size());
		int k = 0;
		for (ClassNode cls : list) {
			String clsRawName = cls.getRawName();
			if (cls.getAccessFlags().isPublic()) {
				NClass nClass = new NClass(clsRawName, k);
				if (names.put(clsRawName, nClass) != null) {
					throw new JadxRuntimeException("Duplicate class: " + clsRawName);
				}
				k++;
			} else {
				names.put(clsRawName, null);
			}
		}
		classes = new NClass[k];
		k = 0;
		for (ClassNode cls : list) {
			if (cls.getAccessFlags().isPublic()) {
				NClass nClass = getCls(cls.getRawName(), names);
				if (nClass == null) {
					throw new JadxRuntimeException("Missing class: " + cls);
				}
				nClass.setParents(makeParentsArray(cls, names));
				classes[k] = nClass;
				k++;
			}
		}
	}

	public static NClass[] makeParentsArray(ClassNode cls, Map<String, NClass> names) {
		List<NClass> parents = new ArrayList<NClass>(1 + cls.getInterfaces().size());
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			NClass c = getCls(superClass.getObject(), names);
			if (c != null) {
				parents.add(c);
			}
		}
		for (ArgType iface : cls.getInterfaces()) {
			NClass c = getCls(iface.getObject(), names);
			if (c != null) {
				parents.add(c);
			}
		}
		return parents.toArray(new NClass[parents.size()]);
	}

	private static NClass getCls(String fullName, Map<String, NClass> names) {
		NClass id = names.get(fullName);
		if (id == null && !names.containsKey(fullName)) {
			LOG.debug("Class not found: {}", fullName);
		}
		return id;
	}

	void save(File output) throws IOException {
		FileUtils.makeDirsForFile(output);

		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output));
		try {
			String outputName = output.getName();
			if (outputName.endsWith(CLST_EXTENSION)) {
				save(outputStream);
			} else if (outputName.endsWith(".jar")) {
				ZipOutputStream out = new ZipOutputStream(outputStream);
				try {
					out.putNextEntry(new ZipEntry(CLST_PKG_PATH + "/" + CLST_FILENAME));
					save(out);
				} finally {
					out.close();
				}
			} else {
				throw new JadxRuntimeException("Unknown file format: " + outputName);
			}
		} finally {
			outputStream.close();
		}
	}

	public void save(OutputStream output) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		try {
			out.writeBytes(JADX_CLS_SET_HEADER);
			out.writeByte(VERSION);

			LOG.info("Classes count: {}", classes.length);
			out.writeInt(classes.length);
			for (NClass cls : classes) {
				writeString(out, cls.getName());
			}
			for (NClass cls : classes) {
				NClass[] parents = cls.getParents();
				out.writeByte(parents.length);
				for (NClass parent : parents) {
					out.writeInt(parent.getId());
				}
			}
		} finally {
			out.close();
		}
	}

	public void load() throws IOException, DecodeException {
		InputStream input = getClass().getResourceAsStream(CLST_FILENAME);
		if (input == null) {
			throw new JadxRuntimeException("Can't load classpath file: " + CLST_FILENAME);
		}
		try {
			load(input);
		} finally {
			input.close();
		}
	}

	public void load(File input) throws IOException, DecodeException {
		String name = input.getName();
		InputStream inputStream = new FileInputStream(input);
		try {
			if (name.endsWith(CLST_EXTENSION)) {
				load(inputStream);
			} else if (name.endsWith(".jar")) {
				ZipInputStream in = new ZipInputStream(inputStream);
				try {
					ZipEntry entry = in.getNextEntry();
					while (entry != null) {
						if (entry.getName().endsWith(CLST_EXTENSION)) {
							load(in);
						}
						entry = in.getNextEntry();
					}
				} finally {
					in.close();
				}
			} else {
				throw new JadxRuntimeException("Unknown file format: " + name);
			}
		} finally {
			inputStream.close();
		}
	}

	public void load(InputStream input) throws IOException, DecodeException {
		DataInputStream in = new DataInputStream(input);
		try {
			byte[] header = new byte[JADX_CLS_SET_HEADER.length()];
			int readHeaderLength = in.read(header);
			int version = in.readByte();
			if (readHeaderLength != JADX_CLS_SET_HEADER.length()
					|| !JADX_CLS_SET_HEADER.equals(new String(header, STRING_CHARSET))
					|| version != VERSION) {
				throw new DecodeException("Wrong jadx class set header");
			}
			int count = in.readInt();
			classes = new NClass[count];
			for (int i = 0; i < count; i++) {
				String name = readString(in);
				classes[i] = new NClass(name, i);
			}
			for (int i = 0; i < count; i++) {
				int pCount = in.readByte();
				NClass[] parents = new NClass[pCount];
				for (int j = 0; j < pCount; j++) {
					parents[j] = classes[in.readInt()];
				}
				classes[i].setParents(parents);
			}
		} finally {
			in.close();
		}
	}

	private void writeString(DataOutputStream out, String name) throws IOException {
		byte[] bytes = name.getBytes(STRING_CHARSET);
		out.writeByte(bytes.length);
		out.write(bytes);
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = in.readByte();
		byte[] bytes = new byte[len];
		int count = in.read(bytes);
		while (count != len) {
			int res = in.read(bytes, count, len - count);
			if (res == -1) {
				throw new IOException("String read error");
			} else {
				count += res;
			}
		}
		return new String(bytes, STRING_CHARSET);
	}

	public int getClassesCount() {
		return classes.length;
	}

	public void addToMap(Map<String, NClass> nameMap) {
		for (NClass cls : classes) {
			nameMap.put(cls.getName(), cls);
		}
	}
}
