package com.voxly.app.data.agora.token;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
