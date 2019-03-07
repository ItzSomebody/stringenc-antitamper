package me.itzsomebody.antitamper;

import java.io.IOException;
import java.util.stream.Stream;
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
import org.objectweb.asm.tree.MethodNode;

public class AntiTamper {
    public static void main(String[] args) {
        File input;
        File output;

        if (args != null) {
            if (args.length == 1) {
                input = new File(args[0]);
                output = new File(args[0].replace(".jar", "") + "-enc.jar");
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
            System.out.println("The input jar: " + input.getAbsolutePath() + " does not exist");
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

        classes.values().forEach(classNode -> {
            Set<MethodNode> toProcess = new HashSet<>();

            classNode.methods.stream().filter(Utils::hasInstructions).forEach(methodNode -> {
                for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                    if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                        methodNode.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                decryptClassName, decryptMethodName, "(Ljava/lang/Object;)Ljava/lang/String;", false));
                        toProcess.add(methodNode);
                    }
                }
            });

            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            ClassReader cr = new ClassReader(cw.toByteArray());

            toProcess.forEach(methodNode -> Stream.of(methodNode.instructions.toArray()).filter(Utils::isString).forEach(insn -> {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                ldc.cst = Utils.encrypt((String) ldc.cst, classNode.name.replace("/", "."), methodNode.name, cr.getItemCount() + 20);
            }));
        });

        classes.values().forEach(classNode -> {
            try {
                ClassWriter cw = new ClassWriter(0);
                classNode.accept(cw);
                for (int i = 0; i < 20; i++)
                    cw.newUTF8(Utils.randomString());

                ZipEntry newEntry = new ZipEntry(classNode.name + ".class");
                newEntry.setTime(current);

                zos.putNextEntry(newEntry);
                zos.write(cw.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        byte[] decryptionBytes = new DecryptorClass(decryptClassName, Utils.randomString(), Utils.randomString(),
                decryptMethodName).getBytes();

        ZipEntry newEntry = new ZipEntry(decryptClassName + ".class");
        newEntry.setTime(current);

        zos.putNextEntry(newEntry);
        zos.write(decryptionBytes);
        zos.close();
    }
}
