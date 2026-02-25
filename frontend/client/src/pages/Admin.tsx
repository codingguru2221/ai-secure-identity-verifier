import { Navbar } from "@/components/layout/Navbar";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from "@/components/ui/table";
import { Activity, ShieldAlert, CheckCircle, Search, Filter } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

// Mock data for the admin dashboard since we don't have a GET endpoint defined
const MOCK_SCANS = [
  { id: "SCAN-8X92-A", date: "2023-10-27 14:32:01", name: "John Doe", type: "Passport", risk: "Low", score: 12 },
  { id: "SCAN-4B11-C", date: "2023-10-27 13:15:44", name: "Jane Smith", type: "Driver License", risk: "Medium", score: 45 },
  { id: "SCAN-9Z77-F", date: "2023-10-27 11:05:12", name: "UNKNOWN", type: "National ID", risk: "High", score: 92 },
  { id: "SCAN-1A22-D", date: "2023-10-26 16:44:33", name: "Robert Robert", type: "Passport", risk: "Low", score: 5 },
  { id: "SCAN-5M33-E", date: "2023-10-26 09:21:10", name: "Alice Wonderland", type: "Driver License", risk: "High", score: 88 },
];

export default function AdminDashboard() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      
      <main className="flex-1 container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-display font-bold">Command Center</h1>
            <p className="text-muted-foreground mt-1">Real-time overview of verification activities</p>
          </div>
          <div className="hidden md:flex items-center gap-2 bg-primary/10 text-primary px-3 py-1.5 rounded-full border border-primary/20 text-sm font-mono">
            <Activity className="w-4 h-4 animate-pulse" />
            SYSTEM_ONLINE
          </div>
        </div>

        {/* Top metrics */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card className="p-6 glass-panel cyber-border">
            <h3 className="text-sm font-mono text-muted-foreground mb-2">Total Scans (24h)</h3>
            <p className="text-4xl font-display font-bold">1,284</p>
            <p className="text-xs text-success mt-2 flex items-center gap-1">
              <span className="font-bold">+12.5%</span> vs yesterday
            </p>
          </Card>
          <Card className="p-6 glass-panel border-success/30 bg-success/5">
            <h3 className="text-sm font-mono text-success/80 mb-2">Passed Verification</h3>
            <p className="text-4xl font-display font-bold text-success">1,102</p>
            <p className="text-xs text-muted-foreground mt-2">85.8% clear rate</p>
          </Card>
          <Card className="p-6 glass-panel border-destructive/30 bg-destructive/5 cyber-glow-destructive">
            <h3 className="text-sm font-mono text-destructive/80 mb-2">Flagged High Risk</h3>
            <p className="text-4xl font-display font-bold text-destructive">47</p>
            <p className="text-xs text-muted-foreground mt-2">Requires manual review</p>
          </Card>
        </div>

        {/* Recent Scans Table */}
        <Card className="glass-panel cyber-border overflow-hidden">
          <div className="p-4 border-b border-border/50 flex flex-col md:flex-row gap-4 justify-between items-center bg-secondary/10">
            <h2 className="font-display font-semibold text-lg">Recent Verifications</h2>
            <div className="flex w-full md:w-auto items-center gap-2">
              <div className="relative w-full md:w-64">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input 
                  placeholder="Search ID or Name..." 
                  className="pl-9 bg-background/50 border-border/50 font-mono text-sm h-9"
                />
              </div>
              <Button variant="outline" size="icon" className="h-9 w-9 border-border/50">
                <Filter className="h-4 w-4 text-muted-foreground" />
              </Button>
            </div>
          </div>
          
          <div className="overflow-x-auto">
            <Table>
              <TableHeader className="bg-background/40 hover:bg-background/40">
                <TableRow className="border-border/50">
                  <TableHead className="font-mono text-xs text-muted-foreground">SCAN ID</TableHead>
                  <TableHead className="font-mono text-xs text-muted-foreground">TIMESTAMP</TableHead>
                  <TableHead className="font-mono text-xs text-muted-foreground">EXTRACTED NAME</TableHead>
                  <TableHead className="font-mono text-xs text-muted-foreground">DOC TYPE</TableHead>
                  <TableHead className="font-mono text-xs text-muted-foreground">RISK LEVEL</TableHead>
                  <TableHead className="font-mono text-xs text-muted-foreground text-right">ACTION</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {MOCK_SCANS.map((scan) => (
                  <TableRow key={scan.id} className="border-border/50 hover:bg-secondary/20 transition-colors">
                    <TableCell className="font-mono text-xs text-primary">{scan.id}</TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">{scan.date}</TableCell>
                    <TableCell className="font-medium">{scan.name}</TableCell>
                    <TableCell className="text-muted-foreground text-sm">{scan.type}</TableCell>
                    <TableCell>
                      {scan.risk === 'High' ? (
                        <Badge variant="outline" className="bg-destructive/10 text-destructive border-destructive/20 gap-1">
                          <ShieldAlert className="w-3 h-3" /> High ({scan.score})
                        </Badge>
                      ) : scan.risk === 'Medium' ? (
                        <Badge variant="outline" className="bg-warning/10 text-warning border-warning/20 gap-1">
                          <Activity className="w-3 h-3" /> Medium ({scan.score})
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="bg-success/10 text-success border-success/20 gap-1">
                          <CheckCircle className="w-3 h-3" /> Low ({scan.score})
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" className="text-xs text-muted-foreground hover:text-primary">
                        View Details
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </Card>
      </main>
    </div>
  );
}
