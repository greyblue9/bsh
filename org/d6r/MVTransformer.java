package org.d6r;
import static bsh.Interpreter.DEBUG;

import org.d6r.AnnotationNode.Item;
import com.googlecode.dex2jar.Method;
import java.util.ArrayDeque;
import java.util.HashSet;

import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute; 
import javassist.bytecode.SignatureAttribute.MethodSignature; 



public class MVTransformer 
  implements 
  SelectTransformer<
    MethodVisitor,
    Pair<MethodVisitor, Collection<String>>
  >
{
  public static final String F_ARRAYLIST_ARRAY
    = CollectionUtil.isJRE()? "elementData": "array";
  
  public static final MVTransformer INSTANCE 
    = new MVTransformer();

  public static final MSigTransformer SIG_INSTANCE 
    = new MSigTransformer();
    
  public static ArrayDeque<Throwable> errors
   = new ArrayDeque<Throwable>();
    
  public static List<Pair<MethodVisitor, MethodSignature>> 
  getSignature(Iterable<? extends MethodVisitor> mvs) {
    try {
      return SIG_INSTANCE.select(INSTANCE.select(mvs));
    } catch (Throwable e) {
      try {
        CollectionUtil.getInterpreter()
          .setu("$_mex", CollectionUtil.clone(e,
            new HashSet<Object>(), 0, 5));
        System.err.println("Exception saved to `$_mex`.");
      } catch (Throwable eIgnore) { }
        
      errors.offerLast(e);
    }
    return Collections.
     <Pair<MethodVisitor,MethodSignature>>emptyList();
  }
  
  public static Pair<MethodVisitor, MethodSignature>
  getSignature(MethodVisitor mv) {
    return SIG_INSTANCE.transform(INSTANCE.transform(mv));
  }
  
  public static class MSigTransformer
  implements   
    SelectTransformer<
    Pair<MethodVisitor, Collection<String>>,
    Pair<MethodVisitor, MethodSignature>
    >
  {
     @Override
     public Pair<MethodVisitor, MethodSignature> 
     transform(Pair<MethodVisitor, Collection<String>> p)
     {
       if (p == null) {
         (new RuntimeException(String.format(
           "Pair p == NULL !!"))).printStackTrace();
         return null;
       }
       
       MethodVisitor mv = /*(MethodVisitor) */ p.getKey();
       if (mv == null) {
         (new RuntimeException(String.format(
           "MethodVisitor mv == NULL !!"))).printStackTrace();
       }
       
       Object _sigstrs = (Object) p.getValue();
       if (DEBUG) Dumper.dump(_sigstrs);
       while (_sigstrs instanceof Collection
        && (((Collection)_sigstrs).size() == 1)) {
         _sigstrs = ((Collection) _sigstrs).iterator().next();
       }
       String sigstrs 
         = (String) (Object) _sigstrs;
       //Dumper.dump(sigstrs);
       String sigstr;
       if (mv.method == null) {
         (new RuntimeException(String.format(
           "mv.method == NULL !!"))).printStackTrace();
       }
       /*if (!(sigstrs instanceof Collection)) {
         System.err.printf(
           "sigstrs: (%s) %s\n",
           sigstrs!=null? sigstrs.getClass().getName():"null",
           Debug.ToString(sigstrs)
         );
       }*/
       
       try {
         sigstr = sigstrs; /*StringUtils.join(
           sigstrs.toArray(new String[0]), ""
         );*/
       } catch (ArrayIndexOutOfBoundsException aioobe) {
         errors.offerLast(aioobe);
         if (DEBUG) System.err.println(
           mv.getName()+": "+aioobe.toString());
         sigstr = mv.method.toString()
           .replaceAll("^[^(]*","");
       } catch (ArrayStoreException ase) {
         errors.offerLast(ase);
         if (DEBUG) System.err.println(
           mv.getName()+": "+ase.toString());
         sigstr = mv.method.toString()
           .replaceAll("^[^(]*","");
       }
       MethodSignature msig;
       try {
         msig = SignatureAttribute.toMethodSignature(sigstr);
       } catch (BadBytecode bbEx) {
         System.err.printf(
           "Bad bytecode input: \n  [%s]\n\n", sigstr
         );
         errors.offerLast(bbEx);
         if ("true".equals(System.getProperty("printStackTrace"))) bbEx.printStackTrace();
         return null; //msig = null;
       }
       return Pair.of(mv, msig);
     }
     
     
     @Override
     public List<Pair<MethodVisitor, MethodSignature>> 
     select(Iterable<? extends
              Pair<MethodVisitor,Collection<String>>> var1)
     {
       ArrayList<Pair<MethodVisitor, MethodSignature>> var2 
         = new ArrayList<>();
       Iterator<? extends 
         Pair<MethodVisitor, Collection<String>>> var3
           = var1.iterator();
  
       while(var3.hasNext()) {
         Pair<MethodVisitor, Collection<String>> var4
           = var3.next();
    
         try {
           var2.add(this.transform(var4));
         } catch (Throwable var6) {
           errors.offerLast(var6);
          ;
         }
       }    
       return var2;
     }
   }
   
   
   @Override
   public Pair<MethodVisitor, Collection<String>>
   transform(MethodVisitor var1) 
   {
     Collection<String> sigstrs;
     
     // Method A (Signature Annotation)
     AnnotationNode sigAn 
       = var1.AVs.get("Ldalvik/annotation/Signature;");
     if (sigAn != null) { 
     
       Object[] strs = (Object[])
         Reflect.getfldval(
           (
             (ArrayList) 
             Reflect.getfldval(
               (
                 (ArrayList)
                 Reflect.getfldval(
                   sigAn, 
                   "items"
                 )
               ).get(0), 
               "value"
             )
           ), 
           F_ARRAYLIST_ARRAY
         );
     
       if (DEBUG) CollectionUtil.getInterpreter()
         .setu("sigAn", sigAn);
       if (DEBUG) CollectionUtil.getInterpreter()
         .setu("var1", var1);
       
       String sig = StringUtils.join(strs, "");
       return Pair.of(
         var1, (Collection<String>) (Object)Arrays.asList(sig)
       );
       //System.err.println("Method A");
       /*return StringUtils.join(((List)Reflect.getfldval(sigAn, "items")).toArray()[0].value, "";
       sigstrs = new ArrayList<String>();
       Dumper.dump(sigstrs);
       int i = -1;
       for (AnnotationNode.Item item: items) {
         i += 1;
         System.err.println(StringUtils.join(
         (Object[]) Reflect.getfldval(item, F_ARRAYLIST_ARRAY), ""
         ));
         sigstrs = (Collection<String>) 
           Arrays.asList(StringUtils.join(
             (Object[]) Reflect.getfldval(item, F_ARRAYLIST_ARRAY), ""
           )); 
         return Pair.of(var1, sigstrs);  
         /*
         Object value = item.value;
         Dumper.dump(value);
         System.err.printf("value = %s\n",
           Debug.ToString(value));
          
         if (value == null) continue; 
         value = StringUtils.join(item.value,"");
         System.err.println("["+String.valueOf(value)+"]");
         sigstrs.add((String)StringUtils.join(value,""));
       }
       if (sigstrs.size() > 0) {
         return Pair.of(var1, (Collection<String>) sigstrs);
       }*/
     }
     
     // Method B
     if (DEBUG) System.err.println("Method B");
     String sigDesc = var1.method.getDesc();
     if (sigDesc == null) {
       sigDesc = var1.method.toString();
     }
     if (sigDesc != null) {
       int parenIdx;
       String ss = (parenIdx = sigDesc.indexOf('(')) != -1
         ? sigDesc.substring(parenIdx)
         : sigDesc;
       return (Pair<MethodVisitor,Collection<String>>)  
        Pair.of(var1, (Collection<String>) Arrays.asList(ss));
     }
     
     // Method C
     if (DEBUG) System.err.println("Method C");
     Method m = var1.method;
     Object _pTypes = m.getParameterTypes();
     Object rType = m.getReturnType();
     Object[] pTypes = new Object[0];
       
     if (_pTypes instanceof Object[]) {
       pTypes = (Object[]) _pTypes;
     } else if (_pTypes instanceof Collection) {
       pTypes = ((Collection<Object>) _pTypes).toArray();
     } else if (_pTypes != null) {
       if (DEBUG) System.err.printf(
         "_pTypes.getClass() -> %s\n",
         _pTypes.getClass().toString()
       );
     }
       
     if (pTypes == null) pTypes = new Object[]{ };
     if (rType == null) rType = "V";
       
     StringBuilder sb = new StringBuilder(76);
     sb.append('(');
     for (Object pType: pTypes) {
       String s = "";
       if (pType == null) {
         if (DEBUG) System.err.println("pType == null"); 
         continue; 
       }
       try {
         s = pType.toString();
       } catch (Throwable e) { 
         errors.offerLast(e);
         if ("true".equals(System.getProperty("printStackTrace"))) e.printStackTrace(); 
         continue; 
       }
       sb.append(s);
     }
     sb.append(')');
     return Pair.of(
         var1, 
         (Collection<String>) (Object) Arrays.asList(
           sb.toString()
         )
     );              
   }
   
   
   @Override
   public List<Pair<MethodVisitor, Collection<String>>>
   select(Iterable<? extends MethodVisitor> var1) {
    ArrayList<Pair<MethodVisitor, Collection<String>>> 
    var2 = new ArrayList<>();
    Iterator<? extends MethodVisitor> var3 = var1.iterator();

    while(var3.hasNext()) {
     MethodVisitor var4 = var3.next();

     try {
       var2.add(transform(var4));
     } catch (Throwable var6) {
       errors.offerLast(var6);
     }
    }

    return var2;
   }
}