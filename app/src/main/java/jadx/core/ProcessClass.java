package jadx.core;

import jadx.core.codegen.CodeGen;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.ErrorsCounter;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.dex.nodes.ProcessState.GENERATED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESSED;
import static jadx.core.dex.nodes.ProcessState.STARTED;
import static jadx.core.dex.nodes.ProcessState.UNLOADED;

public final class ProcessClass {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessClass.class);

	private ProcessClass() {
	}

	public static void process(ClassNode cls, List<IDexTreeVisitor> passes, @Nullable CodeGen codeGen) {
		if (codeGen == null && cls.getState() == PROCESSED) {
			return;
		}
		synchronized (cls) {
			try {
				if (cls.getState() == NOT_LOADED) {
					cls.load();
					cls.setState(STARTED);
					for (IDexTreeVisitor visitor : passes) {
						DepthTraversal.visit(visitor, cls);
					}
					cls.setState(PROCESSED);
				}
				if (cls.getState() == PROCESSED && codeGen != null) {
					processDependencies(cls, passes);
					codeGen.visit(cls);
					cls.setState(GENERATED);
				}
			} catch (Exception e) {
				ErrorsCounter.classError(cls, e.getClass().getSimpleName(), e);
			} finally {
				if (cls.getState() == GENERATED) {
					cls.unload();
					cls.setState(UNLOADED);
				}
			}
		}
	}

	static void processDependencies(ClassNode cls, List<IDexTreeVisitor> passes) {
		for (ClassNode depCls : cls.getDependencies()) {
			process(depCls, passes, null);
		}
	}
}
