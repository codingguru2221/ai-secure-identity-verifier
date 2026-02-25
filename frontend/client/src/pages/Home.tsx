import { useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { VerifyDropzone } from "@/components/verify/Dropzone";
import { VerifyResults } from "@/components/verify/Results";
import { useVerifyIdentity } from "@/hooks/use-verification";
import { useToast } from "@/hooks/use-toast";
import { motion } from "framer-motion";
import { Shield, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function Home() {
  const { toast } = useToast();
  const { mutate: verifyIdentity, isPending, data: result, reset } = useVerifyIdentity();
  const [hasError, setHasError] = useState(false);

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
        {!result && (
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
          {!result && !hasError && (
            <VerifyDropzone onFileSelect={handleFileSelect} isPending={isPending} />
          )}

          {hasError && !isPending && (
            <div className="text-center mt-8">
              <Button onClick={handleReset} variant="outline" className="cyber-border">
                <RefreshCw className="w-4 h-4 mr-2" />
                Try Another Document
              </Button>
            </div>
          )}

          {result && (
            <div className="w-full animate-in fade-in zoom-in duration-500">
              <div className="flex justify-between items-center w-full max-w-4xl mx-auto mb-4">
                <h2 className="text-xl font-display font-semibold">Verification Report</h2>
                <Button onClick={handleReset} variant="ghost" size="sm" className="text-muted-foreground hover:text-foreground">
                  <RefreshCw className="w-4 h-4 mr-2" />
                  New Scan
                </Button>
              </div>
              <VerifyResults result={result} />
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
