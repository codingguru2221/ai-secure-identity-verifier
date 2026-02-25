import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import { UploadCloud, FileType, X, Fingerprint } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { clsx } from "clsx";

interface DropzoneProps {
  onFileSelect: (file: File) => void;
  isPending: boolean;
}

export function VerifyDropzone({ onFileSelect, isPending }: DropzoneProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      setSelectedFile(acceptedFiles[0]);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: 1,
    accept: {
      'image/*': ['.jpeg', '.jpg', '.png', '.webp'],
      'application/pdf': ['.pdf']
    },
    disabled: isPending
  });

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedFile(null);
  };

  const handleVerify = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (selectedFile && !isPending) {
      onFileSelect(selectedFile);
    }
  };

  return (
    <div className="w-full max-w-2xl mx-auto mt-8 relative">
      <div className="absolute inset-0 bg-primary/5 blur-3xl rounded-full pointer-events-none" />
      
      <div 
        {...getRootProps()} 
        className={clsx(
          "relative overflow-hidden group border-2 border-dashed rounded-2xl p-8 md:p-12 transition-all duration-300 ease-out flex flex-col items-center justify-center text-center cursor-pointer min-h-[300px]",
          isDragActive ? "border-primary bg-primary/5 cyber-glow" : "border-border hover:border-primary/50 hover:bg-secondary/30",
          selectedFile ? "border-solid border-primary/30 bg-card/80" : "bg-card/40",
          isPending && "pointer-events-none opacity-80"
        )}
      >
        <input {...getInputProps()} />

        {isPending && <div className="scan-line" />}

        <AnimatePresence mode="wait">
          {!selectedFile ? (
            <motion.div 
              key="empty"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex flex-col items-center gap-4"
            >
              <div className="w-20 h-20 rounded-full bg-secondary/80 flex items-center justify-center mb-2 group-hover:scale-110 transition-transform duration-500 cyber-border">
                <UploadCloud className="w-10 h-10 text-primary/70" />
              </div>
              <div>
                <h3 className="text-xl font-display font-semibold text-foreground">
                  Upload Identity Document
                </h3>
                <p className="text-muted-foreground mt-2 max-w-sm">
                  Drag and drop a passport, driver's license, or national ID. We support JPEG, PNG, and PDF up to 10MB.
                </p>
              </div>
              <Button type="button" variant="outline" className="mt-4 cyber-border hover:bg-primary/10 hover:text-primary">
                Select File
              </Button>
            </motion.div>
          ) : (
            <motion.div 
              key="selected"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex flex-col items-center w-full max-w-sm"
            >
              <div className="relative w-full aspect-video bg-background/50 rounded-xl cyber-border flex flex-col items-center justify-center p-6 mb-6">
                {!isPending && (
                  <Button 
                    type="button"
                    variant="ghost" 
                    size="icon" 
                    className="absolute top-2 right-2 h-8 w-8 rounded-full bg-background/80 hover:bg-destructive/20 hover:text-destructive text-muted-foreground transition-colors z-20"
                    onClick={handleClear}
                  >
                    <X className="w-4 h-4" />
                  </Button>
                )}
                
                {isPending ? (
                  <div className="flex flex-col items-center gap-4">
                    <Fingerprint className="w-12 h-12 text-primary animate-pulse" />
                    <p className="font-mono text-sm text-primary animate-pulse">ANALYZING DOCUMENT...</p>
                  </div>
                ) : (
                  <>
                    <FileType className="w-12 h-12 text-muted-foreground mb-3" />
                    <p className="font-mono text-sm text-foreground truncate max-w-[200px]" title={selectedFile.name}>
                      {selectedFile.name}
                    </p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </>
                )}
              </div>

              {!isPending && (
                <Button 
                  type="button"
                  onClick={handleVerify}
                  className="w-full h-12 text-base font-semibold bg-primary text-primary-foreground hover:bg-primary/90 cyber-glow transition-all"
                >
                  <Fingerprint className="w-5 h-5 mr-2" />
                  Run AI Verification
                </Button>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
