package jadx.core.xmlgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import jadx.api.JadxDecompiler;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.codegen.CodeWriter;

public class ResourcesSaver implements Runnable {
	private final ResourceFile resourceFile;
	private File outDir;
    private static final Logger LOG = LoggerFactory.getLogger(JadxDecompiler.class);

    public ResourcesSaver(File outDir, ResourceFile resourceFile) {
		this.resourceFile = resourceFile;
		this.outDir = outDir;
	}

	@Override
	public void run() {
		if (!ResourceType.isSupportedForUnpack(resourceFile.getType())) {
			return;
		}
		ResContainer rc = resourceFile.getContent();
		if (rc != null) {
			saveResources(rc);
		}
	}

	private void saveResources(ResContainer rc) {
		if (rc == null) {
			return;
		}
		List<ResContainer> subFiles = rc.getSubFiles();
		if (subFiles.isEmpty()) {
			CodeWriter cw = rc.getContent();
			if (cw != null) {
                LOG.info("Processing "+rc.getFileName());
				cw.save(new File(outDir, rc.getFileName()));
			}
		} else {
			for (ResContainer subFile : subFiles) {
				saveResources(subFile);
			}
		}
	}
}
