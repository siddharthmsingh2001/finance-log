import {useRef, useState} from "react";
import {Trash, Upload, User} from "lucide-react";

const ProfilePhotoSelector = ({image, setImage}) => {
    const inputRef = useRef(null);
    const [previewUrl, setPreviewUrl] = useState(null);

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (previewUrl) URL.revokeObjectURL(previewUrl);

            setImage(file);
            const preview = URL.createObjectURL(file);
            setPreviewUrl(preview);
        }
    }

    const handleRemoveImage = (e) => {
        e.preventDefault();
        setImage(null);
        setPreviewUrl(null);
    }

    const onChooseFile = (e) => {
        e.preventDefault();
        inputRef.current?.click();
    }

    return (
        <div className="flex justify-center mb-6">
            <input type="file"
                   accept="image/*"
                   ref={inputRef}
                   onChange={handleImageChange}
                   className="hidden"
            />

            {!image ? (
                <div className="w-24 h-24 flex items-center justify-center bg-purple-100 rounded-full relative">
                    <User className="text-purple-500" size={40} />

                    <button
                        type="button" // Always specify type="button" to prevent form submission
                        onClick={onChooseFile}
                        className="w-10 h-10 flex items-center justify-center bg-purple-600 text-white rounded-full absolute -bottom-1 -right-1 shadow-lg hover:bg-purple-700 transition-transform active:scale-95 border-2 border-white"
                        title="Upload Photo"
                    >
                        <Upload size={20} />
                    </button>
                </div>
            ): (
                <div className="relative">
                    <img src={previewUrl} alt="profile photo" className="w-20 h-20 rounded-full object-cover" />
                    <button
                        onClick={handleRemoveImage}
                        className="w-8 h-8 flex items-center justify-center bg-red-800 text-white rounded-full absolute -bottom-1 -right-1">
                        <Trash size={15}/>
                    </button>
                </div>
            )}
        </div>
    )
}

export default ProfilePhotoSelector;