package li.cil.circuity.vm.devicetree;

public final class DeviceTreeProperty {
    private final String name;
    private final Object[] values;

    public DeviceTreeProperty(final String name, final Object... values) {
        this.name = validateName(name);
        this.values = values;
        for (final Object value : values) {
            if (!(value instanceof String ||
                  value instanceof Integer ||
                  value instanceof Long)) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void flatten(final FlattenedDeviceTree fdt) {
        fdt.property(name, values);
    }

    @Override
    public String toString() {
        if (values == null || values.length == 0) {
            return name + ";";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(name).append(" = ");
            boolean isFirst = true;
            boolean previousWasNumber = false;
            for (final Object current : values) {
                final boolean currentIsNumber = current instanceof Number;
                if (previousWasNumber && !currentIsNumber) {
                    sb.append('>');
                }
                if (!isFirst)
                    if (!previousWasNumber || !currentIsNumber) {
                        sb.append(", ");
                    } else {
                        sb.append(" ");
                    }
                if (currentIsNumber && !previousWasNumber) {
                    sb.append('<');
                }

                isFirst = false;

                if (current instanceof String) {
                    sb.append('"').append((String) current).append('"');
                } else if (current instanceof Integer) {
                    sb.append("0x").append(Integer.toUnsignedString((int) current, 16));
                } else if (current instanceof Long) {
                    sb.append("0x").append(Integer.toUnsignedString((int) (((long) current) >>> 32), 16));
                    sb.append("0x").append(Integer.toUnsignedString((int) ((long) current), 16));
                } else {
                    throw new AssertionError();
                }

                previousWasNumber = currentIsNumber;
            }

            if (previousWasNumber) {
                sb.append('>');
            }

            sb.append(";");

            return sb.toString();
        }
    }

    private static String validateName(final String value) {
        if (value.length() < 1)
            throw new IllegalArgumentException("name too short (<1)");
        if (value.length() > 31)
            throw new IllegalArgumentException("name too long (>31)");
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            if (!isValidCharacterForPropertyName(ch)) {
                throw new IllegalArgumentException("invalid character [" + ch + "] in name [" + value + "]");
            }
        }
        return value;
    }

    private static boolean isValidCharacterForPropertyName(final int ch) {
        return (ch >= '0' && ch <= '9') ||
               (ch >= 'a' && ch <= 'z') ||
               (ch >= 'A' && ch <= 'Z') ||
               ch == ',' || ch == '.' ||
               ch == '_' || ch == '+' ||
               ch == '-' || ch == '?' ||
               ch == '#';
    }
}
