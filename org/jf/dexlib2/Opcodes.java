package org.jf.dexlib2;

import com.google.common.collect.Maps;
import com.google.common.collect.RangeMap;
import java.util.EnumMap;
import java.util.HashMap;

public class Opcodes {

  public final int api;
  public final int artVersion;

  private final EnumMap<Opcode, Short> opcodeValues;
  private final HashMap<String, Opcode> opcodesByName;

  private final Opcode[] opcodesByValue;

  public static Opcodes forApi(int api) {
    return new Opcodes(api, VersionMap.mapApiToArtVersion(api), false);
  }

  public static Opcodes forApi(int api, boolean experimental) {
    return new Opcodes(
      api, VersionMap.mapApiToArtVersion(api), experimental);
  }

  public static Opcodes forArtVersion(int artVersion) {
    return forArtVersion(artVersion, false);
  }

  public static Opcodes forArtVersion(int artVersion, boolean experimental) {
    return new Opcodes(
      VersionMap.mapArtVersionToApi(artVersion), artVersion, experimental);
  }
  
  public static Opcodes getDefault() {
    return forApi(18, true);
  }
  
  public Opcodes(int api, boolean experimental) {
    int version;
    this.opcodesByValue = new Opcode[255];
    this.api = api;
    this.artVersion = VersionMap.mapApiToArtVersion(api);
    this.opcodeValues = new EnumMap(Opcode.class);
    this.opcodesByName = new HashMap<String, Opcode>();
    //if (isArt()) {
    //  version = artVersion;
    //} else {
    version = api;
    //}
    for (Opcode opcode : Opcode.values()) {
      RangeMap<Integer, Short> versionToValueMap;
      if (isArt()) {
        versionToValueMap = opcode.artVersionToValueMap;
      } else {
        versionToValueMap = opcode.apiToValueMap;
      }
      Short opcodeValue = (Short) versionToValueMap.get(Integer.valueOf(version));
      if (opcodeValue != null && (!opcode.isExperimental() || experimental)) {
        if (!opcode.format.isPayloadFormat) {
          this.opcodesByValue[opcodeValue.shortValue()] = opcode;
        }
        this.opcodeValues.put(opcode, opcodeValue);
        this.opcodesByName.put(opcode.name.toLowerCase(), opcode);
      }
    }
  }

  public Opcodes(int api, int artVersion, boolean experimental) {
    int version;
    this.opcodesByValue = new Opcode[255];
    this.api = api;
    this.artVersion = artVersion;
    this.opcodeValues = new EnumMap(Opcode.class);
    this.opcodesByName = new HashMap<String, Opcode>();
    if (isArt()) {
      version = artVersion;
    } else {
      version = api;
    }
    for (Opcode opcode : Opcode.values()) {
      RangeMap<Integer, Short> versionToValueMap;
      if (isArt()) {
        versionToValueMap = opcode.artVersionToValueMap;
      } else {
        versionToValueMap = opcode.apiToValueMap;
      }
      Short opcodeValue 
        = (Short) versionToValueMap.get(Integer.valueOf(version));
      if (opcodeValue != null 
      && (!opcode.isExperimental() || experimental)) 
      {
        if (!opcode.format.isPayloadFormat) {
          this.opcodesByValue[opcodeValue.shortValue()] = opcode;
        }
        this.opcodeValues.put(opcode, opcodeValue);
        this.opcodesByName.put(opcode.name.toLowerCase(), opcode);
      }
    }
  }

  public Opcode getOpcodeByName(String opcodeName) {
    return (Opcode) this.opcodesByName.get(opcodeName.toLowerCase());
  }

  public Opcode getOpcodeByValue(int opcodeValue) {
    switch(opcodeValue) {
      case 256:
        return Opcode.PACKED_SWITCH_PAYLOAD;
      case 512:
        return Opcode.SPARSE_SWITCH_PAYLOAD;
      case 768:
        return Opcode.ARRAY_PAYLOAD;
      default:
        if (opcodeValue < 0 || opcodeValue >= this.opcodesByValue.length) {
          return null;
        }
        return this.opcodesByValue[opcodeValue];
    }
  }

  public Short getOpcodeValue(Opcode opcode) {
    return (Short) this.opcodeValues.get(opcode);
  }

  public boolean isArt() {
    return this.artVersion != -1;
  }
}