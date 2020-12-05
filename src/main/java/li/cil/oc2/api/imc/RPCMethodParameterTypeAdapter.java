package li.cil.oc2.api.imc;

public final class RPCMethodParameterTypeAdapter {
    public final Class<?> type;
    public final Object typeAdapter;

    public RPCMethodParameterTypeAdapter(final Class<?> type, final Object typeAdapter) {
        this.type = type;
        this.typeAdapter = typeAdapter;
    }
}
