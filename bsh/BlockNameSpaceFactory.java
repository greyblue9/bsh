package bsh;

import bsh.BlockNameSpace;
import bsh.Factory;
import org.d6r.SoftHashMap;

public class BlockNameSpaceFactory extends Factory<BlockNameSpace> {
  
  protected static final BlockNameSpaceFactory INSTANCE 
    = new BlockNameSpaceFactory();
  
  static boolean KEY_INCLUDES_CLASS = false;
  
  static final SoftHashMap<Object, BlockNameSpace> reuseCache 
         = new SoftHashMap<Object, BlockNameSpace>(96);
  
  public BlockNameSpaceFactory() {
    this.clazz = BlockNameSpace.class;
  }
  
  public static BlockNameSpaceFactory get() {
    return INSTANCE;
  }
  
  public BlockNameSpace makeWithParent(BshBinding enclosingNameSpace) {
    return new BlockNameSpace(enclosingNameSpace);
  }
  
  public BlockNameSpace getReusableWithParent(
  BshBinding enclosingNameSpace) 
  {
    if (enclosingNameSpace == null) return make();
       
    BlockNameSpace ns = reuseCache.get(enclosingNameSpace);
    
    if (ns == null) {
      reuseCache.put(
        enclosingNameSpace,
        (ns = makeWithParent(enclosingNameSpace))
      );
    }
    return ns;
  }
}
