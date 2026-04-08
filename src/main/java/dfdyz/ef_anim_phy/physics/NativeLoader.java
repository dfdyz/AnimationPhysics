package dfdyz.ef_anim_phy.physics;

import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import dfdyz.ef_anim_phy.EFAnimPhy;
import jme3utilities.MyString;
import net.neoforged.fml.loading.FMLPaths;
import yesman.epicfight.EpicFight;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;

public class NativeLoader {

    public static boolean init(){
        var flavor = "SpMt";
        var path = FMLPaths.CONFIGDIR.get().resolve(EpicFight.MODID).resolve("native");
        var file_name = getLibName("Release", flavor);

        var file = path.resolve(file_name).toFile();
        if(file.exists())
            file.delete();
        try {
            String libpath = MessageFormat.
                    format("/assets/ef_anim_phy/physics_native_lib/{0}", file_name);
            InputStream is = EFAnimPhy.class.getResourceAsStream(libpath);

            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            var resourceBytes = is.readAllBytes();
            fos.write(resourceBytes, 0, resourceBytes.length);
            fos.flush();
            fos.close();
        }catch (Exception e){
            EFAnimPhy.LOGGER.error("Failed to create file: {}\nReason:\n{}", file, e);
        }
        return loadLibbulletjme(true, path.toFile(),"Release", flavor);
    }


    private static String getLibName(String buildType,
                                     String flavor){
        assert buildType.equals("Debug") ||
                buildType.equals("Release") : buildType;

        assert flavor.equals("Sp") ||
                flavor.equals("SpMt") ||
                flavor.equals("SpMtQuickprof") ||
                flavor.equals("SpQuickprof") ||
                flavor.equals("Dp") ||
                flavor.equals("DpMt") : flavor;

        Platform platform = JmeSystem.getPlatform();
        Platform.Os os = platform.getOs();
        String name;
        switch (os) {
            case Android:
            case Linux:
                name = "libbulletjme.so";
                break;
            case MacOS:
                name = "libbulletjme.dylib";
                break;
            case Windows:
                name = "bulletjme.dll";
                break;
            default:
                throw new RuntimeException("platform = " + platform);
        }
        name = platform + buildType + flavor + "_" + name;
        return name;
    }

    private static boolean loadLibbulletjme(boolean dist,
                                           File directory,
                                           String buildType,
                                           String flavor) {
        assert buildType.equals("Debug") ||
                buildType.equals("Release") : buildType;

        assert flavor.equals("Sp") ||
                flavor.equals("SpMt") ||
                flavor.equals("SpMtQuickprof") ||
                flavor.equals("SpQuickprof") ||
                flavor.equals("Dp") ||
                flavor.equals("DpMt") : flavor;

        Platform platform = JmeSystem.getPlatform();
        Platform.Os os = platform.getOs();
        String name;
        switch (os) {
            case Android:
            case Linux:
                name = "libbulletjme.so";
                break;
            case MacOS:
                name = "libbulletjme.dylib";
                break;
            case Windows:
                name = "bulletjme.dll";
                break;
            default:
                throw new RuntimeException("platform = " + platform);
        }

        File file;
        String absoluteFilename;
        if (dist) {
            name = platform + buildType + flavor + "_" + name;
            file = directory;
        } else {
            absoluteFilename = MyString.firstToLower(platform.toString());
            file = new File(directory, absoluteFilename);
            String bt = MyString.firstToLower(buildType);
            file = new File(file, bt);
            String f = MyString.firstToLower(flavor);
            file = new File(file, f);
        }

        file = new File(file, name);
        absoluteFilename = file.getAbsolutePath();
        boolean success = false;
        if (!file.exists()) {
            EFAnimPhy.LOGGER.error("{} does not exist", absoluteFilename);
        } else if (!file.canRead()) {
            EFAnimPhy.LOGGER.error("{} is not readable", absoluteFilename);
        } else {
            EFAnimPhy.LOGGER.info("Loading native library from {}", absoluteFilename);
            System.load(absoluteFilename);
            success = true;
        }

        return success;
    }


}
