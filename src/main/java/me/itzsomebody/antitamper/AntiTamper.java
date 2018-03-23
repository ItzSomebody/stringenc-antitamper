package me.itzsomebody.antitamper;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class AntiTamper {
    public static void main(String[] args) {
        File input;
        File output;

        if (args != null) {
            if (args.length == 1) {
                input = new File(args[0]);
                output = new File(args[0]
                        .replace(".jar", "") + "-enc.jar");
            } else if (args.length == 2) {
                input = new File(args[0]);
                output = new File(args[1]);
            } else {
                System.out.println("Usage: Input.jar <Output.jar>");
                return;
            }
        } else {
            System.out.println("Usage: Input.jar <Output.jar>");
            return;
        }

        if (!input.exists()) {
            System.out.println("The input jar: " + input.getAbsolutePath() +
                    " does not exist");
            return;
        }

        if (output.exists()) {
            System.out.println("Output already exists!");
            return;
        }

        try {
            obf(input, output);
            System.out.println("Finished: " + output.getAbsolutePath());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void obf(File input, File output) throws Throwable {
        ZipFile zipFile = new ZipFile(input);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output));
        Map<String, ClassNode> classes = new HashMap<>();
        long current = System.currentTimeMillis();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                ClassReader cr = new ClassReader(zipFile.getInputStream(entry));
                ClassNode classNode = new ClassNode();

                cr.accept(classNode, 0);
                classes.put(classNode.name, classNode);
            } else {
                ZipEntry newEntry = new ZipEntry(entry);
                newEntry.setTime(current);
                zos.putNextEntry(newEntry);
                zos.write(IOUtils.toByteArray(zipFile.getInputStream(entry)));
            }
        }
        List<String> classNames = new ArrayList<>(classes.keySet());

        String decryptClassName = Utils.randomClassName(classNames);
        String decryptMethodName = Utils.randomString();

        byte[] decryptionBytes = new DecryptorClass(decryptClassName,
                Utils.randomString(), Utils.randomString(), decryptMethodName).getBytes();

        int cpSize = new ClassReader(decryptionBytes).getItemCount();

        classes.values().forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> (methodNode.access & Opcodes.ACC_ABSTRACT) == 0)
                    .forEach(methodNode -> {
                        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                            if (insn instanceof LdcInsnNode
                                    && ((LdcInsnNode) insn).cst instanceof String) {
                                methodNode.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                        decryptClassName, decryptMethodName,
                                        "(Ljava/lang/Object;)Ljava/lang/String;", false));
                                ((LdcInsnNode) insn).cst = Utils.encrypt(((LdcInsnNode) insn).cst.toString(),
                                        decryptClassName.replace("/", "."), decryptMethodName, cpSize);
                            }
                        }
                    });
        });

        for (ClassNode cn : classes.values()) {
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);

            ZipEntry newEntry = new ZipEntry(cn.name + ".class");
            newEntry.setTime(current);

            zos.putNextEntry(newEntry);
            zos.write(cw.toByteArray());
        }

        ZipEntry newEntry = new ZipEntry(decryptClassName + ".class");
        newEntry.setTime(current);

        zos.putNextEntry(newEntry);
        zos.write(decryptionBytes);
        zos.close();
    }
}
