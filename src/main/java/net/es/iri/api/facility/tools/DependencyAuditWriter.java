package net.es.iri.api.facility.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to build a Markdown dependency audit summary combining:
 * - Maven Versions plugin output (target/dependency-updates.txt)
 * - OWASP Dependency-Check JSON report (target/dependency-check-report.json)
 *
 * Usage: run with optional date argument (YYYY-MM-DD). If absent, uses today.
 * Output: dependency-audit-<YYYY-MM-DD>.md in project root.
 */
public class DependencyAuditWriter {

    private record UpdateInfo(String groupId, String artifactId, String current,
                              List<String> candidates, String scope) {}

    private static class VulnInfo {
        String dependency;
        String id;
        String severity;
        String fixedIn; // from vulnerability.fixedIn when available, or blank
        String url;
    }

    public static void main(String[] args) throws Exception {
        String date = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
                ? args[0] : LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Path projectRoot = Paths.get("");
        Path targetDir = projectRoot.resolve("target");
        Path updatesFile = targetDir.resolve("dependency-updates.txt");
        Path depCheckJson = targetDir.resolve("dependency-check-report.json");
        String outName = "dependency-audit-" + date + ".md";
        Path outFile = projectRoot.resolve(outName);

        Map<String, UpdateInfo> updates = parseUpdates(updatesFile);
        List<VulnInfo> vulns = parseVulns(depCheckJson);

        writeMarkdown(outFile, date, updates, vulns);
        System.out.println("Wrote audit summary: " + outFile.toAbsolutePath());
    }

    private static Map<String, UpdateInfo> parseUpdates(Path updatesFile) throws IOException {
        Map<String, UpdateInfo> map = new TreeMap<>();
        if (!Files.exists(updatesFile)) return map;
        Pattern linePattern = Pattern.compile("^\\s*([^:#\n]+):([^:#\s]+)\\s+.*?([0-9A-Za-z._-]+)\\s*->\\s*(.+)$");
        // Example line forms:
        //   com.fasterxml.jackson.core:jackson-databind ................ 2.15.2 -> 2.15.4 -> 2.16.0 -> 2.17.1
        //   org.slf4j:slf4j-api ........................................ 2.0.9 -> 2.0.13
        try (BufferedReader br = new BufferedReader(new FileReader(updatesFile.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = linePattern.matcher(line);
                if (!m.find()) continue;
                String g = m.group(1).trim();
                String a = m.group(2).trim();
                String current = m.group(3).trim();
                String rest = m.group(4).trim();
                // split on arrows and trim
                String[] arr = Arrays.stream(rest.split("->")).map(String::trim).toArray(String[]::new);
                List<String> candidates = new ArrayList<>();
                for (String v : arr) {
                    // stop at non-semver-ish tokens
                    if (v.isEmpty()) continue;
                    candidates.add(v);
                }
                String key = g + ":" + a;
                map.put(key, new UpdateInfo(g, a, current, candidates, "compile"));
            }
        }
        return map;
    }

    private static List<VulnInfo> parseVulns(Path depCheckJson) throws IOException {
        List<VulnInfo> list = new ArrayList<>();
        if (!Files.exists(depCheckJson)) return list;
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(new File(depCheckJson.toString()));
        JsonNode dependencies = root.path("dependencies");
        if (!dependencies.isArray()) return list;
        for (JsonNode dep : dependencies) {
            String fileName = dep.path("fileName").asText("");
            String pkgPath = dep.path("packages").isArray() && dep.path("packages").size() > 0
                    ? dep.path("packages").get(0).path("id").asText("") : "";
            String depName = !pkgPath.isEmpty() ? pkgPath : fileName;
            JsonNode vulns = dep.path("vulnerabilities");
            if (!vulns.isArray()) continue;
            for (JsonNode v : vulns) {
                VulnInfo vi = new VulnInfo();
                vi.dependency = depName;
                vi.id = v.path("name").asText("");
                String severity = v.path("severity").asText("");
                if (severity.isEmpty() && v.has("cvssV3")) {
                    severity = v.path("cvssV3").path("baseSeverity").asText("");
                }
                vi.severity = severity;
                String fixedIn = "";
                if (v.has("fixedIn")) {
                    fixedIn = v.path("fixedIn").toString().replace('"',' ').trim();
                } else if (v.has("vulnerableSoftware")) {
                    // look for versionEndExcluding as hint of fix
                    for (JsonNode vsw : v.path("vulnerableSoftware")) {
                        String endExcl = vsw.path("versionEndExcluding").asText("");
                        if (!endExcl.isBlank()) { fixedIn = ">=" + endExcl; break; }
                    }
                }
                vi.fixedIn = fixedIn;
                String url = "";
                if (v.has("references")) {
                    for (JsonNode ref : v.path("references")) {
                        String u = ref.path("url").asText("");
                        if (u.contains("nvd.nist.gov") || u.contains("github.com/advisories")) { url = u; break; }
                        if (url.isEmpty() && !u.isEmpty()) url = u;
                    }
                }
                vi.url = url;
                list.add(vi);
            }
        }
        return list;
    }

    private static void writeMarkdown(Path outFile, String date, Map<String, UpdateInfo> updates, List<VulnInfo> vulns) throws IOException {
        try (FileWriter fw = new FileWriter(outFile.toFile())) {
            fw.write("# Dependency Audit (" + date + ")\n\n");
            fw.write("Package managers scanned: Maven (pom.xml).\n\n");

            // Vulnerabilities
            fw.write("## Vulnerabilities\n\n");
            if (vulns.isEmpty()) {
                fw.write("No known vulnerabilities detected by OWASP Dependency-Check at scan time.\n\n");
            } else {
                fw.write("ID | Severity | Dependency | Fixed in | Reference\n");
                fw.write("---|---|---|---|---\n");
                // sort by severity then id
                vulns.sort(Comparator.comparing((VulnInfo v) -> sevRank(v.severity)).reversed()
                        .thenComparing(v -> v.id));
                for (VulnInfo v : vulns) {
                    fw.write(String.join(" | ", safe(v.id), safe(v.severity), safe(v.dependency), safe(v.fixedIn), safeLink(v.url)) + "\n");
                }
                fw.write("\n");
            }

            // Updates
            fw.write("## Dependency Updates\n\n");
            if (updates.isEmpty()) {
                fw.write("No updates found by Maven Versions Plugin.\n\n");
            } else {
                Map<String,List<String>> patch = new TreeMap<>();
                Map<String,List<String>> minor = new TreeMap<>();
                Map<String,List<String>> major = new TreeMap<>();
                for (UpdateInfo u : updates.values()) {
                    if (u.candidates().isEmpty()) continue;
                    String bestSameMajor = bestWithinSameMajor(u.current(), u.candidates());
                    if (bestSameMajor == null) {
                        // only majors available; take lowest major candidate for note
                        String cand = u.candidates().get(0);
                        major.put(key(u), List.of(u.current() + " -> " + cand));
                    } else {
                        int cmp = compareSemVer(u.current(), bestSameMajor);
                        if (cmp >= 0) continue; // not actually newer
                        if (isMinorBump(u.current(), bestSameMajor)) {
                            minor.put(key(u), List.of(u.current() + " -> " + bestSameMajor));
                        } else {
                            patch.put(key(u), List.of(u.current() + " -> " + bestSameMajor));
                        }
                        // also record presence of higher major for info
                        String firstMajor = firstHigherMajor(u.current(), u.candidates());
                        if (firstMajor != null) {
                            major.putIfAbsent(key(u), List.of(u.current() + " -> " + firstMajor));
                        }
                    }
                }
                fw.write("### Patch updates (safe)\n\n");
                writeUpdateBlock(fw, patch);
                fw.write("### Minor updates (low risk)\n\n");
                writeUpdateBlock(fw, minor);
                fw.write("### Major updates (skipped/batched)\n\n");
                if (major.isEmpty()) fw.write("None.\n\n");
                else {
                    fw.write("These may introduce breaking changes and are not auto-applied.\n\n");
                    for (var e : major.entrySet()) {
                        fw.write("- " + e.getKey() + ": " + String.join(", ", e.getValue()) + " (potential breaking changes)\n");
                    }
                    fw.write("\n");
                }
            }

            fw.write("## Notes\n\n");
            fw.write("- Major version bumps are listed for awareness and will be handled in a dedicated follow-up due to potential breaking changes.\n");
            fw.write("- To regenerate this report: run scripts/dependency-audit.sh (requires Maven and internet access).\n\n");

            fw.write("## Next steps\n\n");
            fw.write("Would you like me to apply available PATCH/MINOR updates and run the test suite? I will avoid majors.\n");
        }
    }

    private static void writeUpdateBlock(FileWriter fw, Map<String, List<String>> block) throws IOException {
        if (block.isEmpty()) { fw.write("None.\n\n"); return; }
        for (var e : block.entrySet()) {
            fw.write("- " + e.getKey() + ": " + String.join(", ", e.getValue()) + "\n");
        }
        fw.write("\n");
    }

    private static String key(UpdateInfo u) { return u.groupId() + ":" + u.artifactId(); }

    private static int sevRank(String sev) {
        return switch (sev == null ? "" : sev.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 5;
            case "HIGH" -> 4;
            case "MEDIUM", "MODERATE" -> 3;
            case "LOW" -> 2;
            case "INFO", "INFORMATIONAL" -> 1;
            default -> 0;
        };
    }

    private static boolean isMinorBump(String cur, String cand) {
        int[] c1 = parts(cur); int[] c2 = parts(cand);
        return c1[0] == c2[0] && c2[1] > c1[1];
    }

    private static String bestWithinSameMajor(String current, List<String> candidates) {
        int curMajor = parts(current)[0];
        String best = null;
        for (String c : candidates) {
            int[] p = parts(c);
            if (p[0] != curMajor) continue;
            if (best == null || compareSemVer(best, c) < 0) best = c;
        }
        return best;
    }

    private static String firstHigherMajor(String current, List<String> candidates) {
        int curMajor = parts(current)[0];
        for (String c : candidates) {
            if (parts(c)[0] > curMajor) return c;
        }
        return null;
    }

    private static int compareSemVer(String a, String b) {
        int[] pa = parts(a); int[] pb = parts(b);
        if (pa[0] != pb[0]) return Integer.compare(pa[0], pb[0]);
        if (pa[1] != pb[1]) return Integer.compare(pa[1], pb[1]);
        return Integer.compare(pa[2], pb[2]);
    }

    private static int[] parts(String v) {
        // Extract numeric prefix like MAJOR.MINOR.PATCH; ignore qualifiers
        String[] segs = v.split("[.-]");
        int[] out = new int[]{0,0,0};
        for (int i=0;i<Math.min(3,segs.length);i++) {
            try { out[i] = Integer.parseInt(segs[i].replaceAll("[^0-9]","")); } catch (Exception ignored) {}
        }
        return out;
    }

    private static String safe(String s) { return s == null ? "" : s.replace("\n"," "); }
    private static String safeLink(String s) { return s == null || s.isBlank() ? "" : "[link]("+s+")"; }
}
