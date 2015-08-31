package jadx.core.dex.instructions.args;

public final class NamedArg extends InsnArg implements Named {

	private String name;

	public NamedArg( String name,  ArgType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean isNamed() {
		return true;
	}

	public void setName( String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NamedArg)) {
			return false;
		}
		return name.equals(((NamedArg) o).name);

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "(" + name + " " + type + ")";
	}
}
