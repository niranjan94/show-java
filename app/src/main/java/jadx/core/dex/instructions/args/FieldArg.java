package jadx.core.dex.instructions.args;

import jadx.core.dex.info.FieldInfo;

import org.jetbrains.annotations.Nullable;

// TODO: don't extend RegisterArg (now used as a result of instruction)
public final class FieldArg extends RegisterArg {

	private final FieldInfo field;
	// instArg equal 'null' for static fields
	@Nullable
	private final InsnArg instArg;

	public FieldArg(FieldInfo field, @Nullable InsnArg reg) {
		super(-1);
		this.instArg = reg;
		this.field = field;
	}

	public FieldInfo getField() {
		return field;
	}

	public InsnArg getInstanceArg() {
		return instArg;
	}

	public boolean isStatic() {
		return instArg == null;
	}

	@Override
	public boolean isField() {
		return true;
	}

	@Override
	public boolean isRegister() {
		return false;
	}

	@Override
	public void setType(ArgType type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FieldArg) || !super.equals(obj)) {
			return false;
		}
		FieldArg fieldArg = (FieldArg) obj;
		if (!field.equals(fieldArg.field)) {
			return false;
		}
		if (instArg != null ? !instArg.equals(fieldArg.instArg) : fieldArg.instArg != null) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + field.hashCode();
		result = 31 * result + (instArg != null ? instArg.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "(" + field + ")";
	}
}
