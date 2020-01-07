package moe.knox.factorio.library.fileResolver;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.tang.intellij.lua.ext.ILuaFileResolver;

public abstract class FactorioFileResolver implements ILuaFileResolver {
    protected VirtualFile findFile(String shortUrl, VirtualFile root, String[] extensions) {
        for (String ext : extensions) {
            var fixedURL = shortUrl;
            if (shortUrl.endsWith(ext)) { //aa.bb.lua -> aa.bb
                fixedURL = shortUrl.substring(0, shortUrl.length() - ext.length());
            }

            //将.转为/，但不处理 ..
            if (!fixedURL.contains("/")) {
                //aa.bb -> aa/bb
                fixedURL = fixedURL.replace(".", "/");
            }

            fixedURL += ext;

            return VfsUtil.findRelativeFile(root, fixedURL);
        }
        return null;
    }
}
