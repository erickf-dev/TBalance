package sistemainventario.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

/**
 * Pone el ícono de TBalance en alta resolución en la ventana de Edge (barra de
 * tareas y barra de título de Windows).
 *
 * <p>Edge arma el ícono de una ventana de aplicación a partir del ícono de la
 * página en tamaño pequeño (16 px) y Windows lo amplía, por eso se ve borroso.
 * Aquí un proceso de PowerShell, que viene con Windows, busca la ventana
 * "TBalance" de Edge y le asigna un archivo .ico con todos los tamaños
 * (16 a 256 px). Lo repite cada pocos segundos mientras TBalance está abierto,
 * por si Edge vuelve a poner el suyo. Si algo falla, la ventana conserva el
 * ícono de Edge y el programa sigue funcionando.</p>
 */
final class WindowsTaskbarIcon {

    private static final String RECURSO = "/html/img/tbalance.ico";

    /** Código C# que usa la API de Windows (user32) para cambiar el ícono de la ventana. */
    private static final String SCRIPT = String.join("\n",
            "$ErrorActionPreference = 'SilentlyContinue'",
            "Add-Type -TypeDefinition @'",
            "using System;",
            "using System.Text;",
            "using System.Diagnostics;",
            "using System.Runtime.InteropServices;",
            "public static class TbIcono {",
            "  public delegate bool EnumProc(IntPtr h, IntPtr l);",
            "  [DllImport(\"user32.dll\")] public static extern bool EnumWindows(EnumProc f, IntPtr l);",
            "  [DllImport(\"user32.dll\", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);",
            "  [DllImport(\"user32.dll\")] public static extern bool IsWindowVisible(IntPtr h);",
            "  [DllImport(\"user32.dll\")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);",
            "  [DllImport(\"user32.dll\", CharSet = CharSet.Unicode)] public static extern IntPtr LoadImage(IntPtr i, string n, uint t, int cx, int cy, uint f);",
            "  [DllImport(\"user32.dll\")] public static extern IntPtr SendMessage(IntPtr h, uint m, IntPtr w, IntPtr l);",
            "  [DllImport(\"user32.dll\")] public static extern int GetSystemMetrics(int i);",
            "  static IntPtr grande = IntPtr.Zero, chico = IntPtr.Zero;",
            "  public static int Aplicar(string ico) {",
            "    if (grande == IntPtr.Zero) {",
            "      grande = LoadImage(IntPtr.Zero, ico, 1, GetSystemMetrics(11), GetSystemMetrics(12), 0x10);",
            "      chico = LoadImage(IntPtr.Zero, ico, 1, GetSystemMetrics(49), GetSystemMetrics(50), 0x10);",
            "    }",
            "    if (grande == IntPtr.Zero) return -1;",
            "    int n = 0;",
            "    EnumWindows(delegate (IntPtr h, IntPtr l) {",
            "      if (!IsWindowVisible(h)) return true;",
            "      StringBuilder titulo = new StringBuilder(256);",
            "      GetWindowText(h, titulo, 256);",
            "      if (!titulo.ToString().StartsWith(\"TBalance\")) return true;",
            "      uint pid;",
            "      GetWindowThreadProcessId(h, out pid);",
            "      string proceso = \"\";",
            "      try { proceso = Process.GetProcessById((int) pid).ProcessName.ToLower(); } catch { return true; }",
            "      if (proceso != \"msedge\" && proceso != \"chrome\") return true;",
            "      if (SendMessage(h, 0x7F, (IntPtr) 1, IntPtr.Zero) != grande) {",
            "        SendMessage(h, 0x80, (IntPtr) 1, grande);",
            "        SendMessage(h, 0x80, (IntPtr) 0, chico);",
            "      }",
            "      n++;",
            "      return true;",
            "    }, IntPtr.Zero);",
            "    return n;",
            "  }",
            "}",
            "'@",
            "$java = [int] $env:TB_PID",
            "while (Get-Process -Id $java -ErrorAction SilentlyContinue) {",
            "  [TbIcono]::Aplicar($env:TB_ICONO) | Out-Null",
            "  Start-Sleep -Seconds 2",
            "}");

    private WindowsTaskbarIcon() {
    }

    /**
     * Empieza a aplicar el ícono (solo en Windows; en otros sistemas no hace nada).
     *
     * @param carpeta carpeta donde se deja el archivo .ico
     */
    static void aplicar(Path carpeta) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try (InputStream icono = WindowsTaskbarIcon.class.getResourceAsStream(RECURSO)) {
            if (icono == null) {
                return;
            }
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve("tbalance.ico");
            Files.copy(icono, destino, StandardCopyOption.REPLACE_EXISTING);
            String codificado = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-EncodedCommand", codificado);
            pb.environment().put("TB_ICONO", destino.toAbsolutePath().toString());
            pb.environment().put("TB_PID", String.valueOf(ProcessHandle.current().pid()));
            pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException | RuntimeException e) {
            System.err.println("[TBalance] No se pudo poner el ícono en la barra de tareas: " + e.getMessage());
        }
    }
}
