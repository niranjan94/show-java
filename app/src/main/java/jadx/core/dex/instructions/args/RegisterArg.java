package jadx.core.dex.instructions.args;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterArg extends InsnArg implements Named {
	private static final Logger LOG = LoggerFactory.getLogger(RegisterArg.class);

	protected final int regNum;
	// not null after SSATransform pass
	private SSAVar sVar;

	public RegisterArg(int rn) {
		this.regNum = rn;
	}

	public RegisterArg(int rn, ArgType type) {
		this.type = type;
		this.regNum = rn;
	}

	public int getRegNum() {
		return regNum;
	}

	@Override
	public boolean isRegister() {
		return true;
	}

	public SSAVar getSVar() {
		return sVar;
	}

	void setSVar(@NotNull SSAVar sVar) {
		this.sVar = sVar;
	}

	public String getName() {
		if (sVar == null) {
			return null;
		}
		return sVar.getName();
	}

	public void setName(String name) {
		if (sVar != null) {
			sVar.setName(name);
		}
	}

	public boolean isNameEquals(InsnArg arg) {
		String n = getName();
		if (n == null || !(arg instanceof Named)) {
			return false;
		}
		return n.equals(((Named) arg).getName());
	}

	@Override
	public void setType(ArgType type) {
		if (sVar != null) {
			sVar.setType(type);
		}
	}

	public void mergeDebugInfo(ArgType type, String name) {
		setType(type);
		setName(name);
	}

	public RegisterArg duplicate() {
		return duplicate(getRegNum(), sVar);
	}

	public RegisterArg duplicate(int regNum, SSAVar sVar) {
		RegisterArg dup = new RegisterArg(regNum, getType());
		if (sVar != null) {
			dup.setSVar(sVar);
		}
		dup.copyAttributesFrom(this);
		return dup;
	}

	/**
	 * Return constant value from register assign or null if not constant
	 *
	 * @return LiteralArg, String or ArgType
	 */
	public Object getConstValue(DexNode dex) {
		InsnNode parInsn = getAssignInsn();
		if (parInsn == null) {
			return null;
		}
		return InsnUtils.getConstValueByInsn(dex, parInsn);
	}

	@Override
	public boolean isThis() {
		if ("this".equals(getName())) {
			return true;
		}
		// maybe it was moved from 'this' register
		InsnNode ai = getAssignInsn();
		if (ai != null && ai.getType() == InsnType.MOVE) {
			InsnArg arg = ai.getArg(0);
			if (arg != this) {
				return arg.isThis();
			}
		}
		return false;
	}

	public InsnNode getAssignInsn() {
		if (sVar == null) {
			return null;
		}
		return sVar.getAssign().getParentInsn();
	}

	public InsnNode getPhiAssignInsn() {
		PhiInsn usePhi = sVar.getUsedInPhi();
		if (usePhi != null) {
			return usePhi;
		}
		InsnNode parent = sVar.getAssign().getParentInsn();
		if (parent != null && parent.getType() == InsnType.PHI) {
			return parent;
		}
		return null;
	}

	public boolean equalRegisterAndType(RegisterArg arg) {
		return regNum == arg.regNum && type.equals(arg.type);
	}

	@Override
	public int hashCode() {
		return regNum * 31 + type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RegisterArg)) {
			return false;
		}
		RegisterArg other = (RegisterArg) obj;
		if (regNum != other.regNum) {
			return false;
		}
		if (!type.equals(other.type)) {
			return false;
		}
		if (sVar != null && !sVar.equals(other.getSVar())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(r");
		sb.append(regNum);
		if (sVar != null) {
			sb.append("_").append(sVar.getVersion());
		}
		if (getName() != null) {
			sb.append(" '").append(getName()).append("'");
		}
		sb.append(" ");
		sb.append(type);
		sb.append(")");
		return sb.toString();
	}
}
