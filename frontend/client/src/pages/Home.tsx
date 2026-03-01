import { useEffect, useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { VerifyDropzone } from "@/components/verify/Dropzone";
import { VerifyResults } from "@/components/verify/Results";
import { useVerifyIdentity } from "@/hooks/use-verification";
import { useToast } from "@/hooks/use-toast";
import { motion } from "framer-motion";
import { Shield, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { API_ENDPOINTS } from "@/config/api";
import { authHeaders, clearAuthToken, getAuthSession, setAuthSession } from "@/lib/auth";

export default function Home() {
  const { toast } = useToast();
  const { mutate: verifyIdentity, isPending, data: result, reset } = useVerifyIdentity();
  const [hasError, setHasError] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "signup">("login");
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(!!getAuthSession());
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    const session = getAuthSession();
    if (!session) {
      return;
    }

    fetch(API_ENDPOINTS.VALIDATE_TOKEN, { headers: authHeaders() })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error("Token validation failed");
        }
        return response.json();
      })
      .then((data) => {
        if (data?.valid !== true) {
          clearAuthToken();
          setIsAuthenticated(false);
          return;
        }
        setAuthSession({
          token: session.token,
          username: data?.username ?? session.username,
          role: data?.role ?? session.role,
        });
        setIsAuthenticated(true);
      })
      .catch(() => {
        clearAuthToken();
        setIsAuthenticated(false);
      });
  }, []);

  const handleAuth = async () => {
    setAuthLoading(true);
    setAuthError(null);

    try {
      const endpoint = authMode === "login" ? API_ENDPOINTS.LOGIN : API_ENDPOINTS.SIGNUP;
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      const data = await response.json();
      if (!response.ok || !data?.token || !data?.username) {
        throw new Error(authMode === "login" ? "Login failed" : "Signup failed");
      }

      setAuthSession({
        token: data.token,
        username: data.username,
        role: data.role ?? "USER",
      });
      setIsAuthenticated(true);
      setPassword("");
      toast({
        title: authMode === "login" ? "Login successful" : "Signup successful",
        description: `Welcome ${data.username}`,
      });
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Authentication failed");
    } finally {
      setAuthLoading(false);
    }
  };

  const logout = () => {
    clearAuthToken();
    setIsAuthenticated(false);
    setPassword("");
    reset();
  };

  const handleFileSelect = (file: File) => {
    setHasError(false);
    verifyIdentity(file, {
      onError: (error) => {
        setHasError(true);
        toast({
          title: "Verification Error",
          description: error.message,
          variant: "destructive",
        });
      },
      onSuccess: () => {
        toast({
          title: "Scan Complete",
          description: "Document analysis finished successfully.",
          className: "bg-success text-success-foreground border-none",
        });
      }
    });
  };

  const handleReset = () => {
    reset();
    setHasError(false);
  };

  return (
    <div className="min-h-screen flex flex-col relative overflow-hidden">
      {/* Decorative background grid */}
      <div className="absolute inset-0 cyber-grid opacity-30 pointer-events-none z-0" />
      
      <Navbar />

      <main className="flex-1 container mx-auto px-4 py-12 md:py-20 relative z-10 flex flex-col items-center">
        {!result && isAuthenticated && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center max-w-2xl mx-auto mb-10"
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-mono mb-6 cyber-glow">
              <Shield className="w-3 h-3" />
              <span>MILITARY-GRADE ENCRYPTION</span>
            </div>
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-display font-bold mb-4">
              Verify Identities with <span className="text-gradient">Absolute Certainty</span>
            </h1>
            <p className="text-lg text-muted-foreground">
              Upload an ID document. Our AI engine detects forged documents, extracts core entities, and calculates a holistic fraud risk score in seconds.
            </p>
          </motion.div>
        )}

        {/* The main interactive area */}
        <div className="w-full flex-1 flex flex-col items-center justify-center min-h-[400px]">
          {!isAuthenticated && (
            <Card className="w-full max-w-md p-6 glass-panel cyber-border">
              <h2 className="text-2xl font-display font-bold mb-2">
                {authMode === "login" ? "Login Required" : "Create Account"}
              </h2>
              <p className="text-sm text-muted-foreground mb-4">
                Verification records are private and visible only to their owner.
              </p>
              <div className="space-y-3">
                <Input
                  placeholder="Username (letters, numbers, _)"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
                <Input
                  type="password"
                  placeholder="Password (min 8, upper/lower/number)"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                {authError && <p className="text-sm text-destructive">{authError}</p>}
                <Button onClick={handleAuth} disabled={authLoading} className="w-full">
                  {authLoading ? "Please wait..." : authMode === "login" ? "Login" : "Sign Up"}
                </Button>
                <Button
                  variant="ghost"
                  className="w-full"
                  onClick={() => {
                    setAuthError(null);
                    setAuthMode((prev) => (prev === "login" ? "signup" : "login"));
                  }}
                >
                  {authMode === "login" ? "Need an account? Sign up" : "Already have an account? Login"}
                </Button>
              </div>
            </Card>
          )}

          {isAuthenticated && !result && !hasError && (
            <VerifyDropzone onFileSelect={handleFileSelect} isPending={isPending} />
          )}

          {isAuthenticated && hasError && !isPending && (
            <div className="text-center mt-8">
              <Button onClick={handleReset} variant="outline" className="cyber-border">
                <RefreshCw className="w-4 h-4 mr-2" />
                Try Another Document
              </Button>
            </div>
          )}

          {isAuthenticated && result && (
            <div className="w-full animate-in fade-in zoom-in duration-500">
              <div className="flex justify-between items-center w-full max-w-4xl mx-auto mb-4">
                <h2 className="text-xl font-display font-semibold">Verification Report</h2>
                <div className="flex items-center gap-2">
                  <Button onClick={handleReset} variant="ghost" size="sm" className="text-muted-foreground hover:text-foreground">
                    <RefreshCw className="w-4 h-4 mr-2" />
                    New Scan
                  </Button>
                  <Button onClick={logout} variant="outline" size="sm">
                    Logout
                  </Button>
                </div>
              </div>
              <VerifyResults result={result} />
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
