import { ShieldCheck, LayoutDashboard, Search } from "lucide-react";
import { Link, useLocation } from "wouter";
import { clsx } from "clsx";

export function Navbar() {
  const [location] = useLocation();

  return (
    <nav className="sticky top-0 z-50 w-full border-b border-border/40 glass-panel">
      <div className="container mx-auto px-4 h-16 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 group cursor-pointer">
          <div className="relative flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors cyber-border">
            <ShieldCheck className="w-5 h-5 text-primary group-hover:scale-110 transition-transform duration-300" />
          </div>
          <div className="flex flex-col">
            <span className="font-display font-bold text-lg leading-tight tracking-tight text-foreground">
              Secure<span className="text-primary">Verify</span>
            </span>
            <span className="text-[10px] uppercase tracking-widest text-muted-foreground font-mono">
              AI Identity Verification
            </span>
          </div>
        </Link>

        <div className="hidden md:flex items-center gap-1 bg-secondary/50 p-1 rounded-lg border border-border/50">
          <Link 
            href="/" 
            className={clsx(
              "flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200",
              location === "/" 
                ? "bg-background text-foreground shadow-sm cyber-border" 
                : "text-muted-foreground hover:text-foreground hover:bg-secondary"
            )}
          >
            <Search className="w-4 h-4" />
            Scan
          </Link>
          <Link 
            href="/admin" 
            className={clsx(
              "flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all duration-200",
              location === "/admin" 
                ? "bg-background text-foreground shadow-sm cyber-border" 
                : "text-muted-foreground hover:text-foreground hover:bg-secondary"
            )}
          >
            <LayoutDashboard className="w-4 h-4" />
            Dashboard
          </Link>
        </div>
        
        {/* Mobile menu simple version */}
        <div className="flex md:hidden gap-4">
          <Link href="/">
             <Search className={clsx("w-5 h-5", location === "/" ? "text-primary" : "text-muted-foreground")} />
          </Link>
          <Link href="/admin">
             <LayoutDashboard className={clsx("w-5 h-5", location === "/admin" ? "text-primary" : "text-muted-foreground")} />
          </Link>
        </div>
      </div>
    </nav>
  );
}
