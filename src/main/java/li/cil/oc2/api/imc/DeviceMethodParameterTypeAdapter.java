package li.cil.oc2.api.imc;

public final class DeviceMethodParameterTypeAdapter {
    public final Class<?> type;
    public final Object typeAdapter;

    public DeviceMethodParameterTypeAdapter(final Class<?> type, final Object typeAdapter) {
        this.type = type;
        this.typeAdapter = typeAdapter;
    }
}
