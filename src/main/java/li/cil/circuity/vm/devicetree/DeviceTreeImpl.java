package li.cil.circuity.vm.devicetree;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.devicetree.DeviceTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class DeviceTreeImpl implements DeviceTree {
    private final DeviceTreeImpl root;
    private final Object2IntMap<Device> phandles;
    private final IntSet createdPhandles;
    private final MemoryMap mmu;

    public final String name; // node-name
    public final String address; // unit-address
    private final List<DeviceTreeProperty> properties = new ArrayList<>();
    public final List<DeviceTreeImpl> children = new ArrayList<>();

    public DeviceTreeImpl(@Nullable final DeviceTreeImpl parent, final MemoryMap mmu, final String name, final String address) {
        this.root = parent != null ? parent.root : this;
        this.phandles = parent != null ? parent.phandles : new Object2IntArrayMap<>();
        this.createdPhandles = parent != null ? parent.createdPhandles : new IntArraySet();
        this.mmu = mmu;
        this.name = name == null ? "" : validateName(name);
        this.address = address;
    }

    public DeviceTreeImpl(final MemoryMap mmu) {
        this(null, mmu, null, null);
    }

    @Override
    public FlattenedDeviceTree flatten() {
        final FlattenedDeviceTree fdt = new FlattenedDeviceTree();
        flatten(fdt);
        return fdt;
    }

    @Override
    public int createPHandle(final Device device) {
        final int phandle = getPHandle(device);
        createdPhandles.add(phandle);
        return phandle;
    }

    @Override
    public int getPHandle(final Device device) {
        return phandles.computeIntIfAbsent(device, d -> phandles.size() + 1);
    }

    @Override
    public DeviceTree find(final String path) {
        if (path.isEmpty()) {
            return this;
        }

        final int splitIndex = path.indexOf('/');
        if (splitIndex == 0) {
            return root.find(path.substring(1));
        } else {
            if (splitIndex > 0) {
                final String childName = path.substring(0, splitIndex);
                final String pathTail = path.substring(splitIndex + 1);
                return getChild(childName).find(pathTail);
            } else {
                return getChild(path);
            }
        }
    }

    @Override
    public DeviceTree addProp(final String name, final Object... values) {
        properties.add(new DeviceTreeProperty(name, values));
        return this;
    }

    @Override
    public DeviceTree putChild(final String name, final String address, final Consumer<DeviceTree> builder) {
        builder.accept(getChild(name, address));
        return this;
    }

    @Override
    public DeviceTree getChild(final String name, @Nullable final String address) {
        final String fullName = fullName(name, address);
        for (final DeviceTreeImpl child : children) {
            if (fullName.equals(child.fullName())) {
                return child;
            }
        }

        final DeviceTreeImpl child = new DeviceTreeImpl(this, mmu, name, address);
        children.add(child);
        return child;
    }

    private void flatten(final FlattenedDeviceTree fdt) {
        if (address == null) {
            fdt.beginNode(name);
        } else {
            fdt.beginNode(name + "@" + address);
        }

        for (final DeviceTreeProperty property : properties) {
            property.flatten(fdt);
        }

        for (final DeviceTree child : children) {
            if (child instanceof DeviceTreeImpl) {
                final DeviceTreeImpl childBuilder = (DeviceTreeImpl) child;
                childBuilder.flatten(fdt);
            }
        }

        fdt.endNode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }

    private void toString(final StringBuilder sb, final int indent) {
        appendIndent(sb, indent).append(name);
        if (address != null) {
            sb.append('@').append(address);
        }
        sb.append(" {\n");

        for (final DeviceTreeProperty property : properties) {
            appendIndent(sb, indent + 1).append(property.toString()).append('\n');
        }

        for (final DeviceTree child : children) {
            if (child instanceof DeviceTreeImpl) {
                final DeviceTreeImpl childBuilder = (DeviceTreeImpl) child;
                childBuilder.toString(sb, indent + 1);
            } else {
                appendIndent(sb, indent + 1).append(child.toString()).append('\n');
            }
        }

        appendIndent(sb, indent).append("}\n");
    }

    private static StringBuilder appendIndent(final StringBuilder sb, final int indent) {
        for (int i = 0; i < indent * 4; i++) {
            sb.append(' ');
        }
        return sb;
    }

    private String fullName() {
        return fullName(name, address);
    }

    private static String fullName(final String name, @Nullable final String address) {
        return address != null ? String.format("%s@%s", name, address) : name;
    }

    private static String validateName(final String value) {
        if (value.length() < 1)
            throw new IllegalArgumentException("name too short (<1)");
        if (value.length() > 31)
            throw new IllegalArgumentException("name too long (>31)");
        for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            if (!isValidCharacterForNodeName(ch)) {
                throw new IllegalArgumentException("invalid character [" + ch + "] in name [" + value + "]");
            }
        }
        return value;
    }

    private static boolean isValidCharacterForNodeName(final int ch) {
        return (ch >= '0' && ch <= '9') ||
               (ch >= 'a' && ch <= 'z') ||
               (ch >= 'A' && ch <= 'Z') ||
               ch == ',' || ch == '.' ||
               ch == '_' || ch == '+' ||
               ch == '-';
    }
}
