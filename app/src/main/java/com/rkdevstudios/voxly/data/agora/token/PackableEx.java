package com.rkdevstudios.voxly.data.agora.token;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
